package com.buglist.data.repository

import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.entity.PaymentEntity
import com.buglist.domain.model.Payment
import com.buglist.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val debtEntryDao: DebtEntryDao
) : PaymentRepository {

    override fun getPaymentsForDebtEntry(debtEntryId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsForDebtEntry(debtEntryId).map { list ->
            list.map { it.toDomainPayment() }
        }

    override suspend fun getTotalPaidForDebtEntry(debtEntryId: Long): Double =
        paymentDao.getTotalPaidForDebtEntry(debtEntryId)

    override suspend fun insertPaymentAndUpdateStatus(
        payment: Payment,
        newStatus: String
    ): Long {
        val entity = payment.toEntity()
        return paymentDao.insertPaymentAndUpdateStatus(entity, newStatus, debtEntryDao)
    }

    override suspend fun deletePaymentById(paymentId: Long) =
        paymentDao.deleteById(paymentId)
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
