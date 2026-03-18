package com.buglist.domain.usecase

import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Result
import com.buglist.domain.repository.DebtRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case for cancelling a debt entry.
 *
 * Sets status to [DebtStatus.CANCELLED]. Existing payments are preserved for history.
 * A cancelled debt cannot be un-cancelled — it's a terminal state.
 */
class CancelDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(debtEntryId: Long): Result<Unit> {
        val debtWithPayments = debtRepository.getDebtEntryWithPayments(debtEntryId).firstOrNull()
            ?: return Result.Error("Debt entry not found")

        if (debtWithPayments.entry.status == DebtStatus.CANCELLED) {
            return Result.Error("Debt is already cancelled")
        }

        return try {
            debtRepository.updateDebtStatus(debtEntryId, DebtStatus.CANCELLED)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to cancel debt: ${e.message}", e)
        }
    }
}
