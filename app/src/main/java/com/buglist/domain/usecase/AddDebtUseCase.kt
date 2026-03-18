package com.buglist.domain.usecase

import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Result
import com.buglist.domain.repository.DebtRepository
import javax.inject.Inject

/**
 * Use case for creating a new debt entry.
 *
 * Validation rules:
 * - Amount must be > 0.
 * - Amount must not exceed 999,999.99 (UI maximum).
 * - PersonId must be valid (> 0).
 * - Date must be provided (> 0).
 * - The entry is always created with [DebtStatus.OPEN].
 */
class AddDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(debtEntry: DebtEntry): Result<Long> {
        if (debtEntry.personId <= 0) {
            return Result.Error("A valid person must be selected")
        }
        if (debtEntry.amount <= 0.0) {
            return Result.Error("Amount must be greater than 0")
        }
        if (debtEntry.amount > 999_999.99) {
            return Result.Error("Amount must not exceed 999,999.99")
        }
        if (debtEntry.date <= 0) {
            return Result.Error("A valid date must be provided")
        }
        return try {
            val entry = debtEntry.copy(status = DebtStatus.OPEN)
            val id = debtRepository.addDebtEntry(entry)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error("Failed to add debt: ${e.message}", e)
        }
    }
}
