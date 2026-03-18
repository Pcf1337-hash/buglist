package com.buglist.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.MonthlyStats
import com.buglist.presentation.components.AmountText
import com.buglist.presentation.components.PersonAvatar
import com.buglist.presentation.components.StatusChip
import com.buglist.presentation.components.formatAmount
import com.buglist.presentation.theme.BebasNeueFontFamily
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Statistics screen — detailed financial overview with 6 sections.
 *
 * Sections:
 * 1. Gesamt-Überblick (header cards: owed-to-me, I-owe, net balance, open count)
 * 2. Top Schuldner (top 5 who owe the user the most)
 * 3. Top Gläubiger (top 5 the user owes the most)
 * 4. Aktivität / Timeline (last 7 entries)
 * 5. Status-Verteilung (bar-like per-status breakdown)
 * 6. Bezahlte Schulden (total paid history)
 * + Monthly bar chart (letzte 6 Monate)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    onNavigateToPerson: ((personId: Long) -> Unit)? = null,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BugListColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_statistics).uppercase(),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BugListColors.Gold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = BugListColors.Platinum
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BugListColors.Background
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is StatisticsUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator(color = BugListColors.Gold)
                }
            }
            is StatisticsUiState.Ready -> {
                StatisticsContent(
                    data = state.data,
                    paddingValues = paddingValues,
                    onNavigateToPerson = onNavigateToPerson
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    data: StatisticsData,
    paddingValues: PaddingValues,
    onNavigateToPerson: ((personId: Long) -> Unit)?
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // ── Section 1: Gesamt-Überblick ─────────────────────────────────────
        item {
            OverviewSection(data = data)
        }

        // ── Section 2: Top Schuldner ─────────────────────────────────────────
        item {
            TopPersonsSection(
                title = stringResource(R.string.statistics_top_debtors_title),
                items = data.topDebtors,
                amountColor = BugListColors.DebtRed,
                emptyText = stringResource(R.string.statistics_no_debtors),
                onPersonClick = onNavigateToPerson
            )
        }

        // ── Section 3: Top Gläubiger ─────────────────────────────────────────
        item {
            TopPersonsSection(
                title = stringResource(R.string.statistics_top_creditors_title),
                items = data.topCreditors,
                amountColor = BugListColors.DebtGreen,
                emptyText = stringResource(R.string.statistics_no_creditors),
                onPersonClick = onNavigateToPerson
            )
        }

        // ── Section 4: Letzte Aktivität ──────────────────────────────────────
        item {
            ActivitySection(
                entries = data.recentActivity,
                onPersonClick = onNavigateToPerson
            )
        }

        // ── Section 5: Status-Verteilung ─────────────────────────────────────
        item {
            StatusDistributionSection(statusCounts = data.statusCounts)
        }

        // ── Section 6: Bezahlte Schulden ─────────────────────────────────────
        item {
            PaidHistorySection(paidTotals = data.paidTotals)
        }

        // ── Monthly bar chart ─────────────────────────────────────────────────
        item {
            MonthlyBarChart(monthlyStats = data.monthlyStats)
        }
    }
}

// ── Section 1: Gesamt-Überblick ─────────────────────────────────────────────

@Composable
private fun OverviewSection(data: StatisticsData) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(text = stringResource(R.string.statistics_overview_title))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverviewCard(
                label = stringResource(R.string.statistics_owed_to_me),
                amount = data.openTotals.openOwedToMe,
                amountColor = BugListColors.DebtRed,
                modifier = Modifier.weight(1f)
            )
            OverviewCard(
                label = stringResource(R.string.statistics_i_owe),
                amount = data.openTotals.openIOwe,
                amountColor = BugListColors.DebtGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Net balance — positive = good (owed to me), negative = bad (I owe)
            val netBalanceColor = when {
                data.totalBalance > 0 -> BugListColors.DebtRed
                data.totalBalance < 0 -> BugListColors.DebtGreen
                else -> BugListColors.Muted
            }
            OverviewCard(
                label = stringResource(R.string.statistics_net_balance),
                amount = data.totalBalance,
                amountColor = netBalanceColor,
                modifier = Modifier.weight(1f),
                showSign = true
            )
            // Open count card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BugListColors.Surface,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.statistics_open_entries),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = BugListColors.Muted,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = data.openTotals.totalOpenCount.toString(),
                        fontFamily = BebasNeueFontFamily,
                        fontSize = 28.sp,
                        color = BugListColors.Gold
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    label: String,
    amount: Double,
    amountColor: Color,
    modifier: Modifier = Modifier,
    showSign: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BugListColors.Surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = BugListColors.Muted,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            val displayAmount = if (showSign) amount else kotlin.math.abs(amount)
            Text(
                text = formatAmount(displayAmount, "EUR"),
                fontFamily = BebasNeueFontFamily,
                fontSize = 20.sp,
                color = amountColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Section 2 + 3: Top Schuldner / Gläubiger ────────────────────────────────

@Composable
private fun TopPersonsSection(
    title: String,
    items: List<TopDebtorItem>,
    amountColor: Color,
    emptyText: String,
    onPersonClick: ((personId: Long) -> Unit)?
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(text = title)
        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            Text(
                text = emptyText,
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 14.sp,
                color = BugListColors.Muted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BugListColors.Surface
            ) {
                Column {
                    items.forEachIndexed { index, item ->
                        TopPersonRow(
                            item = item,
                            amountColor = amountColor,
                            rank = index + 1,
                            onClick = if (onPersonClick != null) {
                                { onPersonClick(item.person.id) }
                            } else null
                        )
                        if (index < items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = BugListColors.Divider
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopPersonRow(
    item: TopDebtorItem,
    amountColor: Color,
    rank: Int,
    onClick: (() -> Unit)?
) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Rank badge
        Text(
            text = "#$rank",
            fontFamily = BebasNeueFontFamily,
            fontSize = 16.sp,
            color = BugListColors.GoldDim,
            modifier = Modifier.width(28.dp)
        )
        PersonAvatar(
            name = item.person.name,
            avatarColor = item.person.avatarColor,
            size = 36.dp
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = item.person.name,
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BugListColors.Platinum,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatAmount(item.totalAmount, "EUR"),
            fontFamily = BebasNeueFontFamily,
            fontSize = 18.sp,
            color = amountColor
        )
    }
}

// ── Section 4: Letzte Aktivität / Timeline ───────────────────────────────────

@Composable
private fun ActivitySection(
    entries: List<RecentActivityEntry>,
    onPersonClick: ((personId: Long) -> Unit)?
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(text = stringResource(R.string.statistics_activity_title))
        Spacer(Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.statistics_no_activity),
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 14.sp,
                color = BugListColors.Muted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BugListColors.Surface
            ) {
                Column {
                    entries.forEachIndexed { index, activity ->
                        ActivityRow(
                            activity = activity,
                            onClick = if (onPersonClick != null) {
                                { onPersonClick(activity.personId) }
                            } else null
                        )
                        if (index < entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = BugListColors.Divider
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    activity: RecentActivityEntry,
    onClick: (() -> Unit)?
) {
    val dateStr = remember(activity.debtWithPayments.entry.date) {
        SimpleDateFormat("dd.MM.yy", Locale.GERMAN)
            .format(Date(activity.debtWithPayments.entry.date))
    }
    val entry = activity.debtWithPayments.entry
    val amountColor = if (entry.isOwedToMe) BugListColors.DebtRed else BugListColors.DebtGreen

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        PersonAvatar(
            name = activity.personName,
            avatarColor = activity.personAvatarColor,
            size = 32.dp
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.personName,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = BugListColors.Platinum
            )
            Text(
                text = entry.description?.let { "$it · $dateStr" } ?: dateStr,
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 11.sp,
                color = BugListColors.Muted
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatAmount(entry.amount, entry.currency),
                fontFamily = BebasNeueFontFamily,
                fontSize = 16.sp,
                color = amountColor
            )
            StatusChip(status = entry.status)
        }
    }
}

// ── Section 5: Status-Verteilung ─────────────────────────────────────────────

@Composable
private fun StatusDistributionSection(statusCounts: Map<DebtStatus, Int>) {
    val total = statusCounts.values.sum().coerceAtLeast(1)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(text = stringResource(R.string.statistics_status_distribution_title))
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BugListColors.Surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusBarRow(
                    label = stringResource(R.string.status_open),
                    count = statusCounts[DebtStatus.OPEN] ?: 0,
                    total = total,
                    barColor = BugListColors.Gold
                )
                Spacer(Modifier.height(10.dp))
                StatusBarRow(
                    label = stringResource(R.string.status_partial),
                    count = statusCounts[DebtStatus.PARTIAL] ?: 0,
                    total = total,
                    barColor = BugListColors.Orange
                )
                Spacer(Modifier.height(10.dp))
                StatusBarRow(
                    label = stringResource(R.string.status_paid),
                    count = statusCounts[DebtStatus.PAID] ?: 0,
                    total = total,
                    barColor = BugListColors.DebtGreen
                )
                Spacer(Modifier.height(10.dp))
                StatusBarRow(
                    label = stringResource(R.string.status_cancelled),
                    count = statusCounts[DebtStatus.CANCELLED] ?: 0,
                    total = total,
                    barColor = BugListColors.Muted
                )
            }
        }
    }
}

@Composable
private fun StatusBarRow(
    label: String,
    count: Int,
    total: Int,
    barColor: Color
) {
    val fraction = (count.toFloat() / total).coerceIn(0f, 1f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontFamily = RobotoCondensedFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = BugListColors.Platinum,
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BugListColors.SurfaceHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            fontFamily = BebasNeueFontFamily,
            fontSize = 16.sp,
            color = barColor,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

// ── Section 6: Bezahlte Schulden ─────────────────────────────────────────────

@Composable
private fun PaidHistorySection(paidTotals: com.buglist.domain.repository.StatisticsPaidTotals) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(text = stringResource(R.string.statistics_paid_history_title))
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BugListColors.Surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Received back (was owed to me, now PAID)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.statistics_paid_received),
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 13.sp,
                            color = BugListColors.Muted
                        )
                        Text(
                            text = stringResource(R.string.statistics_owed_to_me),
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 11.sp,
                            color = BugListColors.Muted
                        )
                    }
                    AmountText(
                        amount = paidTotals.totalPaidOwedToMe,
                        fontSize = 22.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = BugListColors.Divider)
                Spacer(Modifier.height(12.dp))
                // Paid out (I owed, now PAID)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.statistics_paid_out),
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 13.sp,
                            color = BugListColors.Muted
                        )
                        Text(
                            text = stringResource(R.string.statistics_i_owe),
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 11.sp,
                            color = BugListColors.Muted
                        )
                    }
                    AmountText(
                        amount = -paidTotals.totalPaidIOwe,
                        fontSize = 22.sp
                    )
                }
            }
        }
    }
}

// ── Monthly bar chart ─────────────────────────────────────────────────────────

@Composable
private fun MonthlyBarChart(monthlyStats: List<MonthlyStats>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BugListColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                text = stringResource(R.string.statistics_monthly_chart_title),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (monthlyStats.isEmpty()) {
                Text(
                    text = stringResource(R.string.statistics_no_data),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            } else {
                // Chart legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(BugListColors.DebtRed, RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.statistics_owed_to_me),
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 10.sp,
                        color = BugListColors.Muted
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(BugListColors.DebtGreen, RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.statistics_i_owe),
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 10.sp,
                        color = BugListColors.Muted
                    )
                }

                val modelProducer = remember { CartesianChartModelProducer() }

                LaunchedEffect(monthlyStats) {
                    modelProducer.runTransaction {
                        columnSeries {
                            series(monthlyStats.map { it.totalOwedToMe })
                            series(monthlyStats.map { it.totalIOwe })
                        }
                    }
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(
                                    fill = fill(BugListColors.DebtRed),
                                    thickness = 16.dp,
                                    shape = CorneredShape.rounded(
                                        topLeftPercent = 40,
                                        topRightPercent = 40
                                    )
                                ),
                                rememberLineComponent(
                                    fill = fill(BugListColors.DebtGreen),
                                    thickness = 16.dp,
                                    shape = CorneredShape.rounded(
                                        topLeftPercent = 40,
                                        topRightPercent = 40
                                    )
                                )
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom()
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

// ── Shared UI helpers ─────────────────────────────────────────────────────────

/**
 * Gold Oswald section header used throughout the statistics screen.
 */
@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = BugListColors.Gold,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}
