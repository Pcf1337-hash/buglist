package com.buglist.data.repository

import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.entity.PaymentEntity
import com.buglist.domain.model.Payment
import com.buglist.domain.repository.NoOpSyncScheduler
import com.buglist.domain.repository.PaymentRepository
import com.buglist.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PaymentRepository] backed by Room + SQLCipher.
 *
 * The critical invariant here is that payment inserts and status updates are
 * always performed atomically via [PaymentDao.insertPaymentAndUpdateStatus].
 * See L-035 in lessons.md.
 */
@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
    private val debtEntryDao: DebtEntryDao,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler
) : PaymentRepository {

    override fun getPaymentsForDebtEntry(debtEntryId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsForDebtEntry(debtEntryId).map { list ->
            list.map { it.toDomainPayment() }
        }

    override fun observePaymentChanges(): Flow<Unit> =
        paymentDao.getPaymentCount().mapLatest { }

    override suspend fun getTotalPaidForDebtEntry(debtEntryId: Long): Double =
        paymentDao.getTotalPaidForDebtEntry(debtEntryId)

    override suspend fun insertPaymentAndUpdateStatus(
        payment: Payment,
        newStatus: String
    ): Long {
        val entity = payment.toEntity()
        val id = paymentDao.insertPaymentAndUpdateStatus(entity, newStatus, debtEntryDao)
        val personId = debtEntryDao.getDebtEntryOnce(payment.debtEntryId)?.personId
        if (personId != null) {
            syncScheduler.afterPaymentChanged(id, payment.debtEntryId, personId)
        }
        return id
    }

    override suspend fun deletePaymentById(paymentId: Long) {
        val debtId = paymentDao.debtIdOf(paymentId)
        val personId = debtId?.let { debtEntryDao.getDebtEntryOnce(it)?.personId }
        syncScheduler.beforePaymentDelete(paymentId)
        paymentDao.deleteById(paymentId)
        if (personId != null) syncScheduler.afterLocalChange(personId)
    }
}

// --- Mapping extensions ---

internal fun PaymentEntity.toDomainPayment(): Payment = Payment(
    id = id,
    debtEntryId = debtEntryId,
    amount = amount,
    note = note,
    date = date
)

internal fun Payment.toEntity(): PaymentEntity = PaymentEntity(
    id = id,
    debtEntryId = debtEntryId,
    amount = amount,
    note = note,
    date = date
)
