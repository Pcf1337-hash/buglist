package com.buglist.domain.usecase

import com.buglist.domain.model.Payment
import com.buglist.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case that returns a live stream of all payments for a specific debt entry.
 *
 * Payments are ordered newest-first (repository responsibility).
 */
class GetPaymentsForDebtUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(debtEntryId: Long): Flow<List<Payment>> =
        paymentRepository.getPaymentsForDebtEntry(debtEntryId)
}
