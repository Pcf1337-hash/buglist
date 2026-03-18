package com.buglist.domain.usecase

import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.Result
import com.buglist.domain.repository.DebtRepository
import javax.inject.Inject

/**
 * Use case for updating an existing debt entry.
 *
 * Preserves the entry's current status — only amount, description, direction,
 * currency, date, and dueDate are editable. The id and personId are immutable.
 *
 * Validation rules:
 * - Entry id must be > 0 (existing entry).
 * - Amount must be > 0.
 * - Amount must not exceed 999,999.99.
 * - Date must be > 0.
 */
class UpdateDebtEntryUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(debtEntry: DebtEntry): Result<Unit> {
        if (debtEntry.id <= 0) {
            return Result.Error("Entry id must be valid for an update")
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
            debtRepository.updateDebtEntry(debtEntry)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update debt: ${e.message}", e)
        }
    }
}
