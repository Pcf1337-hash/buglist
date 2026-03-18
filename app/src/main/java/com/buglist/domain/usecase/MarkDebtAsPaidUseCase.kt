package com.buglist.domain.usecase

import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Payment
import com.buglist.domain.model.Result
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case that marks a debt as fully paid by recording a payment for the remaining amount.
 *
 * Creates a payment equal to [DebtEntryWithPayments.remaining] and sets status to [DebtStatus.PAID].
 * If the debt is already paid or cancelled, returns [Result.Error].
 */
class MarkDebtAsPaidUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(debtEntryId: Long): Result<Unit> {
        val debtWithPayments = debtRepository.getDebtEntryWithPayments(debtEntryId).firstOrNull()
            ?: return Result.Error("Debt entry not found")

        if (debtWithPayments.entry.status == DebtStatus.PAID) {
            return Result.Error("Debt is already fully paid")
        }
        if (debtWithPayments.entry.status == DebtStatus.CANCELLED) {
            return Result.Error("Cannot mark a cancelled debt as paid")
        }

        val remaining = debtWithPayments.remaining
        if (remaining <= 0.0) {
            // Already at 0 but status not updated — force status correction.
            return try {
                debtRepository.updateDebtStatus(debtEntryId, DebtStatus.PAID)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error("Failed to update status: ${e.message}", e)
            }
        }

        val fullPayment = Payment(
            debtEntryId = debtEntryId,
            amount = remaining,
            note = null,
            date = System.currentTimeMillis()
        )

        return try {
            paymentRepository.insertPaymentAndUpdateStatus(
                payment = fullPayment,
                newStatus = DebtStatus.PAID.name
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to mark as paid: ${e.message}", e)
        }
    }
}
