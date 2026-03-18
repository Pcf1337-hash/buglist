package com.buglist.domain.usecase

import com.buglist.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case that returns the global net balance across all persons as a live stream.
 *
 * Balance is computed from **remaining** amounts (original minus payments) for
 * non-terminal debts only (OPEN and PARTIAL). PAID and CANCELLED are excluded.
 *
 * Positive = total owed to the user.
 * Negative = total the user owes others.
 */
class CalculateTotalBalanceUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    operator fun invoke(): Flow<Double> = debtRepository.getTotalNetBalance()
}
