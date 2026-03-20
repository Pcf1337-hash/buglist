package com.buglist.domain.repository

import com.buglist.domain.model.Payment
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for [Payment] operations.
 *
 * This interface lives in the domain layer — it knows nothing about Room, DAOs,
 * or any data-layer implementation details.
 *
 * The implementation is in [com.buglist.data.repository.PaymentRepositoryImpl].
 *
 * Key invariant: after every payment insertion, the parent [com.buglist.domain.model.DebtEntry]
 * status must be updated atomically in the same transaction (L-035 in lessons.md).
 */
interface PaymentRepository {

    /**
     * Returns a live stream of all payments for a debt entry, ordered by date descending.
     */
    fun getPaymentsForDebtEntry(debtEntryId: Long): Flow<List<Payment>>

    /**
     * Returns a reactive stream that emits whenever any payment is inserted or deleted.
     *
     * Used as a combine-trigger in ViewModels to force pipeline re-evaluation after
     * settlement — necessary because SQLCipher's @Transaction handling can miss Room's
     * InvalidationTracker notifications for @Relation tables. (L-039)
     */
    fun observePaymentChanges(): Flow<Unit>

    /**
     * Returns the total amount paid for a debt entry.
     * Returns 0.0 if no payments exist.
     */
    suspend fun getTotalPaidForDebtEntry(debtEntryId: Long): Double

    /**
     * Atomically inserts a payment and updates the parent debt entry's status.
     *
     * The status is computed by the caller ([AddPartialPaymentUseCase]) based on
     * the new remaining amount. This method just persists both changes in one transaction.
     *
     * @param payment   The payment to insert.
     * @param newStatus "PARTIAL" or "PAID" — computed before calling this method.
     * @return          The auto-generated payment ID.
     */
    suspend fun insertPaymentAndUpdateStatus(
        payment: Payment,
        newStatus: String
    ): Long

    /**
     * Deletes a payment by ID.
     * Note: the caller is responsible for recomputing and updating the debt entry status.
     */
    suspend fun deletePaymentById(paymentId: Long)
}
