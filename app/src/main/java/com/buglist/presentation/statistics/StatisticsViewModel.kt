package com.buglist.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.MonthlyStats
import com.buglist.domain.model.Person
import com.buglist.domain.model.PersonWithBalance
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.StatisticsOpenTotals
import com.buglist.domain.repository.StatisticsPaidTotals
import com.buglist.domain.usecase.CalculateTotalBalanceUseCase
import com.buglist.domain.usecase.GetPersonsWithBalancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/**
 * A recent debt entry enriched with the person's name and avatar color.
 * Used by the statistics activity timeline section.
 */
data class RecentActivityEntry(
    val debtWithPayments: DebtEntryWithPayments,
    val personName: String,
    val personAvatarColor: Int,
    val personId: Long
)

/** Top debtor item: person + total amount they owe (or I owe them). */
data class TopDebtorItem(
    val person: Person,
    val totalAmount: Double
)

/**
 * All data assembled by [StatisticsViewModel] for the statistics screen.
 */
data class StatisticsData(
    /** Monthly totals for the bar chart (last 6 months). */
    val monthlyStats: List<MonthlyStats>,
    /** Top 5 persons who owe the user the most (netBalance > 0). */
    val topDebtors: List<TopDebtorItem>,
    /** Top 5 persons the user owes the most (netBalance < 0). */
    val topCreditors: List<TopDebtorItem>,
    /** Global net balance across all persons. */
    val totalBalance: Double,
    /** Open-amount totals split by direction + open entry count. */
    val openTotals: StatisticsOpenTotals,
    /** Paid-amount totals for the history section. */
    val paidTotals: StatisticsPaidTotals,
    /** Count of entries per status. */
    val statusCounts: Map<DebtStatus, Int>,
    /** Last 7 entries for the activity timeline. */
    val recentActivity: List<RecentActivityEntry>
)

sealed class StatisticsUiState {
    object Loading : StatisticsUiState()
    data class Ready(val data: StatisticsData) : StatisticsUiState()
}

/**
 * Intermediate grouping type to keep the nested combine readable.
 */
private data class CoreStats(
    val monthly: List<MonthlyStats>,
    val persons: List<PersonWithBalance>,
    val totalBalance: Double,
    val openTotals: StatisticsOpenTotals,
    val paidTotals: StatisticsPaidTotals
)

/**
 * ViewModel for the statistics screen.
 *
 * Combines multiple repository flows into a single [StatisticsUiState] via nested [combine] calls.
 * All heavy computation happens here — the Composable only reads state.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val getPersonsWithBalancesUseCase: GetPersonsWithBalancesUseCase,
    private val calculateTotalBalanceUseCase: CalculateTotalBalanceUseCase
) : ViewModel() {

    private val sixMonthsAgo: Long = run {
        Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
    }

    /** First combine: monthly + persons + totalBalance + openTotals + paidTotals */
    private val coreStats = combine(
        debtRepository.getMonthlyStats(sixMonthsAgo),
        getPersonsWithBalancesUseCase(),
        calculateTotalBalanceUseCase(),
        debtRepository.getOpenTotals(),
        debtRepository.getPaidTotals()
    ) { monthly, persons, totalBalance, openTotals, paidTotals ->
        CoreStats(
            monthly = monthly,
            persons = persons,
            totalBalance = totalBalance,
            openTotals = openTotals,
            paidTotals = paidTotals
        )
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        coreStats,
        debtRepository.getStatusCounts(),
        debtRepository.getLatestEntries(7)
    ) { core, statusCounts, latestEntries ->

        // Build a person lookup map for the timeline
        val personMap: Map<Long, Person> = core.persons.associate { it.person.id to it.person }

        // Top 5 debtors — persons who owe the user the most (netBalance > 0)
        val topDebtors = core.persons
            .filter { it.netBalance > 0 }
            .sortedByDescending { it.netBalance }
            .take(5)
            .map { TopDebtorItem(it.person, it.netBalance) }

        // Top 5 creditors — persons the user owes the most (netBalance < 0)
        val topCreditors = core.persons
            .filter { it.netBalance < 0 }
            .sortedBy { it.netBalance }   // most negative first
            .take(5)
            .map { TopDebtorItem(it.person, -it.netBalance) } // positive for display

        // Activity timeline: enrich with person names
        val recentActivity = latestEntries.mapNotNull { debtWithPayments ->
            val person = personMap[debtWithPayments.entry.personId] ?: return@mapNotNull null
            RecentActivityEntry(
                debtWithPayments = debtWithPayments,
                personName = person.name,
                personAvatarColor = person.avatarColor,
                personId = person.id
            )
        }

        StatisticsUiState.Ready(
            StatisticsData(
                monthlyStats = core.monthly,
                topDebtors = topDebtors,
                topCreditors = topCreditors,
                totalBalance = core.totalBalance,
                openTotals = core.openTotals,
                paidTotals = core.paidTotals,
                statusCounts = statusCounts,
                recentActivity = recentActivity
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState.Loading
    )
}
