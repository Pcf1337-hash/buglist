package com.buglist.domain.usecase

import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Payment
import com.buglist.domain.model.Result
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case for recording a partial (or full) payment on a debt entry.
 *
 * Validation rules (see L-034 in lessons.md):
 * - Payment amount must be > 0.
 * - Payment amount must be <= remaining (prevents overpayment).
 * - The debt entry must exist and be in OPEN or PARTIAL state.
 *
 * After recording the payment, the debt status is automatically updated:
 * - remaining == 0 → [DebtStatus.PAID]
 * - remaining > 0  → [DebtStatus.PARTIAL]
 *
 * The payment insert and status update happen atomically in one transaction (L-035).
 */
class AddPartialPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(payment: Payment): Result<Long> {
        if (payment.debtEntryId <= 0) {
            return Result.Error("Invalid debt entry reference")
        }
        if (payment.amount <= 0.0) {
            return Result.Error("Payment amount must be greater than 0")
        }

        // Load the current state of the debt entry to validate remaining amount.
        val debtWithPayments = debtRepository.getDebtEntryWithPayments(payment.debtEntryId)
            .firstOrNull()
            ?: return Result.Error("Debt entry not found")

        if (debtWithPayments.entry.status == DebtStatus.PAID) {
            return Result.Error("This debt is already fully paid")
        }
        if (debtWithPayments.entry.status == DebtStatus.CANCELLED) {
            return Result.Error("Cannot add payment to a cancelled debt")
        }

        // Guard: payment must not exceed remaining (L-034)
        if (payment.amount > debtWithPayments.remaining + 0.001) {
            // +0.001 tolerance for floating-point rounding
            return Result.Error(
                "Payment (${payment.amount}) exceeds remaining balance (${debtWithPayments.remaining})"
            )
        }

        // Compute the new status after this payment.
        val newRemaining = maxOf(0.0, debtWithPayments.remaining - payment.amount)
        val newStatus = if (newRemaining < 0.001) DebtStatus.PAID else DebtStatus.PARTIAL

        return try {
            val id = paymentRepository.insertPaymentAndUpdateStatus(
                payment = payment,
                newStatus = newStatus.name
            )
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error("Failed to record payment: ${e.message}", e)
        }
    }
}
