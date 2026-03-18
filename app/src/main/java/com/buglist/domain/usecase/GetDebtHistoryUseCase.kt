package com.buglist.domain.usecase

import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Filter configuration for debt history.
 *
 * @param statuses    Filter by these statuses. Empty list = all statuses.
 * @param fromDate    Only include entries with date >= fromDate. 0 = no filter.
 * @param toDate      Only include entries with date <= toDate. 0 = no filter.
 */
data class DebtHistoryFilter(
    val statuses: List<DebtStatus> = emptyList(),
    val fromDate: Long = 0L,
    val toDate: Long = 0L
)

/**
 * Use case that returns filtered debt history for a specific person.
 *
 * Filtering by date is done in-process since the query already returns a live flow.
 * Status filtering is pushed to the DB query.
 */
class GetDebtHistoryUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    operator fun invoke(
        personId: Long,
        filter: DebtHistoryFilter = DebtHistoryFilter()
    ): Flow<List<DebtEntryWithPayments>> {
        val statuses = filter.statuses.ifEmpty { DebtStatus.entries }
        return debtRepository.getDebtEntriesForPersonByStatus(personId, statuses)
            .map { list ->
                list.filter { dwp ->
                    val date = dwp.entry.date
                    val afterFrom = filter.fromDate <= 0 || date >= filter.fromDate
                    val beforeTo = filter.toDate <= 0 || date <= filter.toDate
                    afterFrom && beforeTo
                }
            }
    }
}
