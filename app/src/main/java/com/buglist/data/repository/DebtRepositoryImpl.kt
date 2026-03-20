package com.buglist.data.repository

import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.relation.DebtEntryWithPaymentsRelation
import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.MonthlyStats
import com.buglist.domain.model.Payment
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.StatisticsOpenTotals
import com.buglist.domain.repository.StatisticsPaidTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DebtRepository] backed by Room + SQLCipher.
 *
 * All mapping between domain models and entities is contained in this class.
 */
@Singleton
class DebtRepositoryImpl @Inject constructor(
    private val debtEntryDao: DebtEntryDao
) : DebtRepository {

    /**
     * In-memory version counter incremented on every write.
     *
     * Room's InvalidationTracker is unreliable with SQLCipher 4.9.0 for simple
     * @Insert/@Update calls — notifications sometimes don't fire. This counter
     * bypasses the InvalidationTracker entirely and guarantees instant notification
     * to all observers after any debt-entry write. (L-043)
     */
    private val _changeVersion = MutableStateFlow(0L)

    override fun observeDebtEntryChanges(): Flow<Unit> = _changeVersion.map { }

    override fun getDebtEntriesWithPaymentsForPerson(personId: Long): Flow<List<DebtEntryWithPayments>> =
        debtEntryDao.getDebtEntriesWithPaymentsForPerson(personId).map { list ->
            list.map { it.toDomain() }
        }

    override fun getDebtEntriesForPersonByStatus(
        personId: Long,
        statuses: List<DebtStatus>
    ): Flow<List<DebtEntryWithPayments>> =
        debtEntryDao.getDebtEntriesWithPaymentsForPerson(personId).map { list ->
            val statusNames = statuses.map { it.name }
            list.filter { statusNames.isEmpty() || it.debtEntry.status in statusNames }
                .map { it.toDomain() }
        }

    override fun getDebtEntryWithPayments(debtEntryId: Long): Flow<DebtEntryWithPayments?> =
        debtEntryDao.getDebtEntryWithPayments(debtEntryId).map { it?.toDomain() }

    override fun getRecentDebtEntries(
        statuses: List<DebtStatus>,
        limit: Int
    ): Flow<List<DebtEntryWithPayments>> =
        debtEntryDao.getRecentDebtEntries(
            statuses = statuses.map { it.name },
            limit = limit
        ).map { list -> list.map { it.toDomain() } }

    override fun getTotalNetBalance(): Flow<Double> =
        debtEntryDao.getTotalNetBalance()

    override fun getMonthlyStats(fromTimestamp: Long): Flow<List<MonthlyStats>> =
        debtEntryDao.getMonthlyTotals(fromTimestamp).map { rows ->
            rows.map { MonthlyStats(it.yearMonth, it.totalOwedToMe, it.totalIOwe) }
        }

    override fun searchByDescription(query: String): Flow<List<DebtEntryWithPayments>> =
        debtEntryDao.searchByDescription(query).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun addDebtEntry(debtEntry: DebtEntry): Long {
        val id = debtEntryDao.insert(debtEntry.toEntity())
        _changeVersion.update { it + 1 }
        return id
    }

    override suspend fun updateDebtEntry(debtEntry: DebtEntry) {
        debtEntryDao.update(debtEntry.toEntity())
        _changeVersion.update { it + 1 }
    }

    override suspend fun deleteDebtEntry(debtEntry: DebtEntry) {
        debtEntryDao.delete(debtEntry.toEntity())
        _changeVersion.update { it + 1 }
    }

    override suspend fun deleteDebtEntryById(debtEntryId: Long) {
        debtEntryDao.deleteById(debtEntryId)
        _changeVersion.update { it + 1 }
    }

    override suspend fun updateDebtStatus(debtEntryId: Long, status: DebtStatus) {
        debtEntryDao.updateStatus(debtEntryId, status.name)
        _changeVersion.update { it + 1 }
    }

    override suspend fun getOpenDebtsForPerson(
        personId: Long,
        isOwedToMe: Boolean
    ): List<DebtEntryWithPayments> =
        debtEntryDao.getOpenDebtsForPersonOrderedByDate(personId, isOwedToMe)
            .map { it.toDomain() }

    override fun getOpenTotals(): Flow<StatisticsOpenTotals> =
        debtEntryDao.getOpenTotals().map { row ->
            StatisticsOpenTotals(
                openOwedToMe = row.openOwedToMe,
                openIOwe = row.openIOwe,
                totalOpenCount = row.totalOpenCount
            )
        }

    override fun getPaidTotals(): Flow<StatisticsPaidTotals> =
        debtEntryDao.getPaidTotals().map { row ->
            StatisticsPaidTotals(
                totalPaidOwedToMe = row.totalPaidOwedToMe,
                totalPaidIOwe = row.totalPaidIOwe
            )
        }

    override fun getStatusCounts(): Flow<Map<DebtStatus, Int>> =
        debtEntryDao.getStatusCounts().map { rows ->
            rows.associate { row ->
                DebtStatus.fromString(row.status) to row.count
            }
        }

    override fun getLatestEntries(limit: Int): Flow<List<DebtEntryWithPayments>> =
        debtEntryDao.getLatestEntries(limit).map { list ->
            list.map { it.toDomain() }
        }
}

// --- Mapping extensions ---

internal fun DebtEntryEntity.toDomain(): DebtEntry = DebtEntry(
    id = id,
    personId = personId,
    amount = amount,
    currency = currency,
    isOwedToMe = isOwedToMe,
    description = description,
    date = date,
    dueDate = dueDate,
    status = DebtStatus.fromString(status),
    createdAt = createdAt
)

internal fun DebtEntry.toEntity(): DebtEntryEntity = DebtEntryEntity(
    id = id,
    personId = personId,
    amount = amount,
    currency = currency,
    isOwedToMe = isOwedToMe,
    description = description,
    date = date,
    dueDate = dueDate,
    status = status.name,
    createdAt = createdAt
)

internal fun DebtEntryWithPaymentsRelation.toDomain(): DebtEntryWithPayments {
    val entry = debtEntry.toDomain()
    val domainPayments = payments.map { it.toDomainPayment() }
    return DebtEntryWithPayments.from(entry, domainPayments)
}
