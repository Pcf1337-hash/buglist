package com.buglist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.buglist.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [PaymentEntity].
 *
 * Reads return [Flow] for reactive UI updates.
 * Writes are `suspend fun` — must be called from a coroutine context.
 *
 * Status updates after payment insertion are handled via [DebtEntryDao.updateStatus]
 * inside a [Transaction] in the repository layer (see L-035 in lessons.md).
 */
@Dao
interface PaymentDao {

    /** Inserts a payment record. Returns the auto-generated row ID. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: PaymentEntity): Long

    /** Deletes a specific payment by ID. */
    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deleteById(paymentId: Long)

    /** Returns all payments for a debt entry, ordered by date descending. */
    @Query("""
        SELECT * FROM payments
        WHERE debtEntryId = :debtEntryId
        ORDER BY date DESC
    """)
    fun getPaymentsForDebtEntry(debtEntryId: Long): Flow<List<PaymentEntity>>

    /**
     * Returns the sum of all payments for a debt entry.
     * Returns 0.0 if no payments exist.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM payments
        WHERE debtEntryId = :debtEntryId
    """)
    suspend fun getTotalPaidForDebtEntry(debtEntryId: Long): Double

    /**
     * Atomically inserts a payment and updates the debt entry status.
     *
     * This transaction ensures the database never has a payment without a
     * corresponding status update — preventing the PARTIAL/PAID inconsistency
     * described in L-035 of lessons.md.
     *
     * @param payment   The payment to record.
     * @param newStatus The computed new status ("PARTIAL" or "PAID") after this payment.
     */
    /**
     * Atomically inserts a payment and updates the debt entry status.
     * Returns the auto-generated payment row ID.
     *
     * This transaction ensures the database never has a payment without a
     * corresponding status update — preventing the PARTIAL/PAID inconsistency
     * described in L-035 of lessons.md.
     *
     * @param payment   The payment to record.
     * @param newStatus The computed new status ("PARTIAL" or "PAID") after this payment.
     * @return          The auto-generated row ID of the inserted payment.
     */
    @Transaction
    suspend fun insertPaymentAndUpdateStatus(
        payment: PaymentEntity,
        newStatus: String,
        debtEntryDao: DebtEntryDao
    ): Long {
        val id = insert(payment)
        debtEntryDao.updateStatus(payment.debtEntryId, newStatus)
        return id
    }
}
