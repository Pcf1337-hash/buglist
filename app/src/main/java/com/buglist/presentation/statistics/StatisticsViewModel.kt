package com.buglist.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.MonthlyStats
import com.buglist.domain.model.Person
import com.buglist.domain.model.PersonWithBalance
import com.buglist.domain.repository.DayActivityCount
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.StatisticsOpenTotals
import com.buglist.domain.repository.StatisticsPaidTotals
import com.buglist.domain.usecase.CalculateTotalBalanceUseCase
import com.buglist.domain.usecase.GetPersonsWithBalancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.concurrent.TimeUnit
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

/** Top debtor item: person + total amount they owe (or I owe them) + reliability score. */
data class TopDebtorItem(
    val person: Person,
    val totalAmount: Double,
    /** Reliability score 0–100: (paidCount / totalCount) * 100. */
    val reliabilityScore: Int = 0
)

/** Time range selection for the statistics chart. */
enum class TimeRange(val labelRes: Int, val days: Int) {
    WEEK(com.buglist.R.string.statistics_timerange_7d, 7),
    MONTH(com.buglist.R.string.statistics_timerange_30d, 30),
    SIX_MONTHS(com.buglist.R.string.statistics_timerange_6m, 180),
    ALL(com.buglist.R.string.statistics_timerange_all, Int.MAX_VALUE)
}

/**
 * Insight cards generated from statistics data.
 */
sealed class Insight {
    /** A warning: person hasn't paid in a long time, or other risk signal. */
    data class Warning(val message: String, val personName: String? = null, val personId: Long? = null) : Insight()
    /** An informational insight: concentration risk, etc. */
    data class Info(val message: String) : Insight()
    /** A positive celebration: debt-free, milestone, etc. */
    data class Celebration(val message: String) : Insight()
}

/**
 * All data assembled by [StatisticsViewModel] for the statistics screen.
 */
data class StatisticsData(
    /** Monthly totals for the bar chart (filtered by [selectedTimeRange]). */
    val monthlyStats: List<MonthlyStats>,
    /** Top 5 persons who owe the user the most (netBalance > 0). */
    val topDebtors: List<TopDebtorItem>,
    /** Top 5 persons the user owes the most (netBalance < 0). */
    val topCreditors: List<TopDebtorItem>,
    /** Global net balance across all persons. */
    val totalBalance: Double,
    /** Previous month net balance for delta calculation. */
    val prevMonthBalance: Double,
    /** Open-amount totals split by direction + open entry count. */
    val openTotals: StatisticsOpenTotals,
    /** Paid-amount totals for the history section. */
    val paidTotals: StatisticsPaidTotals,
    /** Count of entries per status. */
    val statusCounts: Map<DebtStatus, Int>,
    /** Last 7 entries for the activity timeline. */
    val recentActivity: List<RecentActivityEntry>,
    /** At-risk amount: open debts > 60 days (owed to me). */
    val atRiskAmount: Double,
    /** Average days an open debt has been outstanding. */
    val avgDebtDurationDays: Double,
    /** Overall repayment rate 0.0–1.0. */
    val repaymentRate: Double,
    /** Auto-generated insight cards. */
    val insights: List<Insight>,
    /** Daily activity counts for the heatmap (last 91 days). */
    val activityData: Map<Long, DayActivityCount>,
    /** Currently selected time range for the chart. */
    val selectedTimeRange: TimeRange
)

sealed class StatisticsUiState {
    object Loading : StatisticsUiState()
    data class Ready(val data: StatisticsData) : StatisticsUiState()
}

/**
 * Intermediate grouping type to keep the nested combine readable (L-086).
 */
private data class CoreStats(
    val monthly: List<MonthlyStats>,
    val persons: List<PersonWithBalance>,
    val totalBalance: Double,
    val openTotals: StatisticsOpenTotals,
    val paidTotals: StatisticsPaidTotals
)

private data class ExtendedStats(
    val atRiskAmount: Double,
    val avgDurationDays: Double,
    val repaymentRate: Double,
    val reliabilityScores: Map<Long, Int>
)

/**
 * ViewModel for the statistics screen.
 *
 * Combines multiple repository flows into a single [StatisticsUiState] via nested [combine] calls.
 * Uses two levels of nesting to stay within the 5-flow combine limit (L-086).
 * All heavy computation happens here — the Composable only reads state.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val getPersonsWithBalancesUseCase: GetPersonsWithBalancesUseCase,
    private val calculateTotalBalanceUseCase: CalculateTotalBalanceUseCase
) : ViewModel() {

    /** Currently selected time range — driven by the Segmented Control. */
    val selectedTimeRange = MutableStateFlow(TimeRange.SIX_MONTHS)

    private val sixMonthsAgo: Long = run {
        Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
    }

    private val sixtyDaysAgo: Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60)

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

    /** Second combine: at-risk + avgDuration + repaymentRate + reliabilityScores */
    private val extendedStats = combine(
        debtRepository.getAtRiskAmount(sixtyDaysAgo),
        debtRepository.getAvgDebtDurationDays(),
        debtRepository.getRepaymentRate(),
        debtRepository.getPersonReliabilityScores()
    ) { atRisk, avgDuration, repaymentRate, reliabilityScores ->
        ExtendedStats(
            atRiskAmount = atRisk,
            avgDurationDays = avgDuration,
            repaymentRate = repaymentRate,
            reliabilityScores = reliabilityScores
        )
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        coreStats,
        extendedStats,
        debtRepository.getStatusCounts(),
        debtRepository.getLatestEntries(7),
        combine(
            debtRepository.getActivityByDay(91),
            selectedTimeRange
        ) { activity, range -> Pair(activity, range) }
    ) { core, extended, statusCounts, latestEntries, (activityData, timeRange) ->

        // Build a person lookup map for the timeline
        val personMap: Map<Long, Person> = core.persons.associate { it.person.id to it.person }

        // Top 5 debtors — persons who owe the user the most (netBalance > 0)
        val topDebtors = core.persons
            .filter { it.netBalance > 0 }
            .sortedByDescending { it.netBalance }
            .take(5)
            .map { TopDebtorItem(it.person, it.netBalance, extended.reliabilityScores[it.person.id] ?: 0) }

        // Top 5 creditors — persons the user owes the most (netBalance < 0)
        val topCreditors = core.persons
            .filter { it.netBalance < 0 }
            .sortedBy { it.netBalance }   // most negative first
            .take(5)
            .map { TopDebtorItem(it.person, -it.netBalance, extended.reliabilityScores[it.person.id] ?: 0) }

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

        // Filter monthly stats based on selected time range
        val filteredMonthly = filterMonthlyStats(core.monthly, timeRange)

        // Previous month balance for delta (simplified: compare first vs last monthly entry)
        val prevMonthBalance = if (core.monthly.size >= 2) {
            val prev = core.monthly[core.monthly.size - 2]
            prev.totalOwedToMe - prev.totalIOwe
        } else 0.0

        // Generate insights
        val insights = generateInsights(
            persons = core.persons,
            topDebtors = topDebtors,
            totalBalance = core.totalBalance,
            openTotals = core.openTotals,
            atRiskAmount = extended.atRiskAmount
        )

        StatisticsUiState.Ready(
            StatisticsData(
                monthlyStats = filteredMonthly,
                topDebtors = topDebtors,
                topCreditors = topCreditors,
                totalBalance = core.totalBalance,
                prevMonthBalance = prevMonthBalance,
                openTotals = core.openTotals,
                paidTotals = core.paidTotals,
                statusCounts = statusCounts,
                recentActivity = recentActivity,
                atRiskAmount = extended.atRiskAmount,
                avgDebtDurationDays = extended.avgDurationDays,
                repaymentRate = extended.repaymentRate,
                insights = insights,
                activityData = activityData,
                selectedTimeRange = timeRange
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState.Loading
    )

    /** Called from UI when user taps a Segmented Control button. */
    fun onTimeRangeSelected(range: TimeRange) {
        selectedTimeRange.value = range
    }

    private fun filterMonthlyStats(
        all: List<MonthlyStats>,
        range: TimeRange
    ): List<MonthlyStats> {
        if (range == TimeRange.ALL || all.isEmpty()) return all
        val monthsBack = when (range) {
            TimeRange.WEEK -> 1
            TimeRange.MONTH -> 1
            TimeRange.SIX_MONTHS -> 6
            TimeRange.ALL -> Int.MAX_VALUE
        }
        return all.takeLast(monthsBack.coerceAtMost(all.size))
    }

    /**
     * Generates insight cards from the statistics data.
     * Rules (in priority order):
     * - Warning if top person hasn't triggered any recent activity (simplified: if atRiskAmount > 0)
     * - Info if top 2 debtors hold > 75% of total open volume
     * - Celebration if totalBalance > 0
     * - Celebration if no open debts
     */
    private fun generateInsights(
        persons: List<PersonWithBalance>,
        topDebtors: List<TopDebtorItem>,
        totalBalance: Double,
        openTotals: StatisticsOpenTotals,
        atRiskAmount: Double
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // At-risk warning: significant amount overdue > 60 days
        if (atRiskAmount > 0.01) {
            val formatted = String.format(java.util.Locale.GERMAN, "%.0f €", atRiskAmount)
            insights.add(Insight.Warning("⚠ $formatted seit über 60 Tagen offen"))
        }

        // Concentration / Pareto risk: top 2 persons hold > 75% of open volume
        val totalOpen = openTotals.openOwedToMe
        if (topDebtors.size >= 2 && totalOpen > 0.01) {
            val top2Share = (topDebtors[0].totalAmount + topDebtors[1].totalAmount) / totalOpen
            if (top2Share > 0.75) {
                val pct = (top2Share * 100).toInt()
                val names = "${topDebtors[0].person.name} & ${topDebtors[1].person.name}"
                insights.add(Insight.Info("💡 Klumpenrisiko: $names halten $pct% deiner Forderungen"))
            }
        }

        // No open debts at all — celebrate
        if (openTotals.totalOpenCount == 0) {
            insights.add(Insight.Celebration("🔥 Du bist schuldenfrei"))
        } else if (totalBalance > 0.01) {
            val formatted = String.format(java.util.Locale.GERMAN, "%.2f €", totalBalance)
                .replace('.', ',')
            insights.add(Insight.Celebration("🎉 $formatted stehen dir zu"))
        }

        return insights
    }
}
