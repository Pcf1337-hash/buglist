package com.buglist.domain.repository

import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.MonthlyStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for [DebtEntry] operations.
 *
 * This interface lives in the domain layer — it knows nothing about Room, DAOs,
 * or any data-layer implementation details.
 *
 * The implementation is in [com.buglist.data.repository.DebtRepositoryImpl].
 */
interface DebtRepository {

    /**
     * Returns a live stream of all debt entries with their payments for a specific person,
     * ordered by date descending.
     */
    fun getDebtEntriesWithPaymentsForPerson(personId: Long): Flow<List<DebtEntryWithPayments>>

    /**
     * Returns a live stream of debt entries for a person filtered by [statuses].
     * Used for tab filtering (OPEN+PARTIAL / PAID / ALL).
     */
    fun getDebtEntriesForPersonByStatus(
        personId: Long,
        statuses: List<DebtStatus>
    ): Flow<List<DebtEntryWithPayments>>

    /**
     * Returns a single debt entry with its payments as a live stream.
     * Returns null if the entry does not exist.
     */
    fun getDebtEntryWithPayments(debtEntryId: Long): Flow<DebtEntryWithPayments?>

    /**
     * Returns recent debt entries across all persons for the dashboard feed.
     * Limited to the most recently created entries with the given [statuses].
     */
    fun getRecentDebtEntries(
        statuses: List<DebtStatus>,
        limit: Int = 20
    ): Flow<List<DebtEntryWithPayments>>

    /**
     * Returns the global net balance across all persons as a live stream.
     * Positive = net owed to the user. Negative = net the user owes.
     */
    fun getTotalNetBalance(): Flow<Double>

    /**
     * Returns monthly debt totals for the last 6 months for the statistics chart.
     */
    fun getMonthlyStats(fromTimestamp: Long): Flow<List<MonthlyStats>>

    /**
     * Searches debt entries by description text across all persons.
     */
    fun searchByDescription(query: String): Flow<List<DebtEntryWithPayments>>

    /**
     * Inserts a new debt entry. Returns the auto-generated row ID.
     */
    suspend fun addDebtEntry(debtEntry: DebtEntry): Long

    /**
     * Updates an existing debt entry.
     */
    suspend fun updateDebtEntry(debtEntry: DebtEntry)

    /**
     * Deletes a debt entry and all associated payments (CASCADE DELETE).
     */
    suspend fun deleteDebtEntry(debtEntry: DebtEntry)

    /**
     * Deletes a debt entry by ID (CASCADE DELETE).
     */
    suspend fun deleteDebtEntryById(debtEntryId: Long)

    /**
     * Deletes ALL debt entries for a person (CASCADE deletes their payments).
     * Used by the import use case to replace the complete list during sync.
     */
    suspend fun deleteAllDebtsForPerson(personId: Long)

    /**
     * Updates only the status of a debt entry.
     * Called by [PaymentRepository] inside a transaction.
     */
    suspend fun updateDebtStatus(debtEntryId: Long, status: DebtStatus)

    /**
     * Returns all open (OPEN or PARTIAL) debt entries for a person, sorted by date ascending.
     *
     * Used by [com.buglist.domain.usecase.SettleDebtsUseCase] to build the FIFO settlement queue.
     * Oldest entry first so debts are settled in chronological order.
     *
     * @param personId    The person whose debts to load.
     * @param isOwedToMe  true = entries where the person owes the user; false = entries where the
     *                    user owes the person.
     */
    suspend fun getOpenDebtsForPerson(
        personId: Long,
        isOwedToMe: Boolean
    ): List<DebtEntryWithPayments>

    /**
     * Returns a reactive stream that emits whenever any debt entry is inserted, updated, or deleted.
     *
     * Used as a combine-trigger in ViewModels to force pipeline re-evaluation after creating
     * or editing a debt entry — necessary because SQLCipher's @Transaction handling can miss
     * Room's InvalidationTracker notifications for @Relation tables. (L-040)
     */
    fun observeDebtEntryChanges(): Flow<Unit>

    /**
     * Returns open amount totals split by direction and the total open entry count.
     * Used by the statistics header cards.
     */
    fun getOpenTotals(): Flow<StatisticsOpenTotals>

    /**
     * Returns totals of fully paid (PAID status) entries.
     * Used for the "Bezahlte Schulden" section in statistics.
     */
    fun getPaidTotals(): Flow<StatisticsPaidTotals>

    /**
     * Returns the count of entries per status.
     * Used for the status distribution section in statistics.
     */
    fun getStatusCounts(): Flow<Map<DebtStatus, Int>>

    /**
     * Returns the latest [limit] entries (newest first) for the activity timeline.
     */
    fun getLatestEntries(limit: Int = 7): Flow<List<DebtEntryWithPayments>>

    /**
     * Returns the sum of open "owed-to-me" debt amounts where the debt is older than [thresholdMs].
     * Used for the At-Risk card in statistics (default: 60 days ago).
     */
    fun getAtRiskAmount(thresholdMs: Long): Flow<Double>

    /**
     * Returns the average duration in days of currently open/partial debts.
     * Used for the Ø-Dauer bento tile.
     */
    fun getAvgDebtDurationDays(): Flow<Double>

    /**
     * Returns the overall repayment rate as a value 0.0–1.0.
     * Computed as paidCount / totalCount (CANCELLED excluded).
     */
    fun getRepaymentRate(): Flow<Double>

    /**
     * Returns a map of personId → reliabilityScore (0–100) for all persons.
     * Score = (paidCount / totalCount) * 100. CANCELLED entries excluded.
     */
    fun getPersonReliabilityScores(): Flow<Map<Long, Int>>

    /**
     * Returns daily activity data for the heatmap covering the last [days] days.
     * Key = day epoch ms (midnight UTC), value = [DayActivityCount].
     */
    fun getActivityByDay(days: Int = 91): Flow<Map<Long, DayActivityCount>>
}

/** Aggregated open-debt totals used by the statistics screen header. */
data class StatisticsOpenTotals(
    val openOwedToMe: Double,
    val openIOwe: Double,
    val totalOpenCount: Int
)

/** Aggregated paid-debt totals for the statistics history section. */
data class StatisticsPaidTotals(
    val totalPaidOwedToMe: Double,
    val totalPaidIOwe: Double
)

/** Activity count for a single day in the heatmap. */
data class DayActivityCount(
    val paymentCount: Int,
    val newDebtCount: Int
) {
    val total: Int get() = paymentCount + newDebtCount
}
