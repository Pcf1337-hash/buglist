package com.buglist.presentation.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.MonthlyStats
import com.buglist.domain.repository.DayActivityCount
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Statistics screen — Street-Dashboard redesign.
 *
 * Layout (top to bottom in LazyColumn):
 * 1. Hero Balance with animated Bebas Neue counter + delta vs. prev month
 * 2. Segmented Control (7T / 30T / 6M / ALLE)
 * 3. Insight-Cards as horizontal LazyRow
 * 4. Bento-Grid: At-Risk (full width) + Repayment Rate + Ø Duration
 * 5. Monthly Bar Chart (filtered by time range)
 * 6. Top Debtors / Creditors with Reliability Score rings
 * 7. Activity Heatmap (7×13 GitHub-style grid)
 * 8. Status Distribution
 * 9. Paid History
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
                    onNavigateToPerson = onNavigateToPerson,
                    onTimeRangeSelected = viewModel::onTimeRangeSelected
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    data: StatisticsData,
    paddingValues: PaddingValues,
    onNavigateToPerson: ((personId: Long) -> Unit)?,
    onTimeRangeSelected: (TimeRange) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // ── 1. Hero Balance ─────────────────────────────────────────────────────
        item {
            HeroBalanceSection(
                totalBalance = data.totalBalance,
                prevMonthBalance = data.prevMonthBalance
            )
        }

        // ── 2. Segmented Control ────────────────────────────────────────────────
        item {
            TimeRangeSegmentedControl(
                selectedRange = data.selectedTimeRange,
                onRangeSelected = onTimeRangeSelected
            )
        }

        // ── 3. Insight Cards ────────────────────────────────────────────────────
        if (data.insights.isNotEmpty()) {
            item {
                InsightCardsRow(insights = data.insights)
            }
        }

        // ── 4. Bento Grid: At-Risk + Key Metrics ────────────────────────────────
        item {
            BentoGridSection(
                atRiskAmount = data.atRiskAmount,
                repaymentRate = data.repaymentRate,
                avgDurationDays = data.avgDebtDurationDays,
                openOwedToMe = data.openTotals.openOwedToMe,
                openIOwe = data.openTotals.openIOwe,
                totalOpenCount = data.openTotals.totalOpenCount
            )
        }

        // ── 5. Monthly Bar Chart ────────────────────────────────────────────────
        item {
            MonthlyBarChart(monthlyStats = data.monthlyStats)
        }

        // ── 6. Top Debtors with Reliability Rings ───────────────────────────────
        item {
            TopPersonsSection(
                title = stringResource(R.string.statistics_top_debtors_title),
                items = data.topDebtors,
                amountColor = BugListColors.DebtGreen,
                emptyText = stringResource(R.string.statistics_no_debtors),
                onPersonClick = onNavigateToPerson
            )
        }

        item {
            TopPersonsSection(
                title = stringResource(R.string.statistics_top_creditors_title),
                items = data.topCreditors,
                amountColor = BugListColors.DebtRed,
                emptyText = stringResource(R.string.statistics_no_creditors),
                onPersonClick = onNavigateToPerson
            )
        }

        // ── 7. Activity Heatmap ─────────────────────────────────────────────────
        item {
            ActivityHeatmapSection(activityData = data.activityData)
        }

        // ── 8. Status Distribution ──────────────────────────────────────────────
        item {
            StatusDistributionSection(statusCounts = data.statusCounts)
        }

        // ── 9. Paid History ─────────────────────────────────────────────────────
        item {
            PaidHistorySection(paidTotals = data.paidTotals)
        }

        // ── 10. Recent Activity Timeline ────────────────────────────────────────
        item {
            ActivitySection(
                entries = data.recentActivity,
                onPersonClick = onNavigateToPerson
            )
        }
    }
}

// ── 1. Hero Balance ─────────────────────────────────────────────────────────────

@Composable
private fun HeroBalanceSection(
    totalBalance: Double,
    prevMonthBalance: Double
) {
    val animatedBalance = remember { Animatable(0f) }

    LaunchedEffect(totalBalance) {
        animatedBalance.animateTo(
            targetValue = totalBalance.toFloat(),
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    val delta = totalBalance - prevMonthBalance
    val deltaColor = when {
        delta > 0 -> BugListColors.DebtGreen
        delta < 0 -> BugListColors.DebtRed
        else -> BugListColors.Muted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Currency superscript + big number
        val displayValue = animatedBalance.value.toDouble()
        val isNegative = displayValue < 0
        val absValue = abs(displayValue)
        val euros = absValue.toLong()
        val cents = ((absValue - euros) * 100).roundToInt()

        val heroText = buildAnnotatedString {
            // currency symbol as superscript
            withStyle(
                SpanStyle(
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Muted,
                    baselineShift = BaselineShift.Superscript
                )
            ) {
                append(if (isNegative) "-€" else "€")
            }
            withStyle(
                SpanStyle(
                    fontFamily = BebasNeueFontFamily,
                    fontSize = 88.sp,
                    color = if (isNegative) BugListColors.DebtRed else BugListColors.Gold,
                )
            ) {
                append(String.format(Locale.GERMAN, "%,d", euros))
            }
            withStyle(
                SpanStyle(
                    fontFamily = BebasNeueFontFamily,
                    fontSize = 36.sp,
                    color = if (isNegative) BugListColors.DebtRed else BugListColors.Gold,
                )
            ) {
                append(String.format(Locale.GERMAN, ",%02d", cents))
            }
        }

        Text(
            text = heroText,
            modifier = Modifier.wrapContentWidth(Alignment.Start)
        )

        // Delta vs previous month
        if (delta != 0.0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = if (delta > 0) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = deltaColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatAmount(abs(delta), "EUR"),
                    fontFamily = BebasNeueFontFamily,
                    fontSize = 15.sp,
                    color = deltaColor
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.statistics_hero_delta_vs_prev),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 12.sp,
                    color = BugListColors.Muted
                )
            }
        }
    }
}

// ── 2. Segmented Control ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeSegmentedControl(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    val ranges = TimeRange.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ranges.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BugListColors.Gold.copy(alpha = 0.15f),
                    activeContentColor = BugListColors.Gold,
                    activeBorderColor = BugListColors.Gold,
                    inactiveContainerColor = BugListColors.Surface,
                    inactiveContentColor = BugListColors.Muted,
                    inactiveBorderColor = BugListColors.Divider
                )
            ) {
                Text(
                    text = stringResource(range.labelRes),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── 3. Insight Cards ────────────────────────────────────────────────────────────

@Composable
private fun InsightCardsRow(insights: List<Insight>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(text = stringResource(R.string.statistics_insights_title))
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(insights) { insight ->
                InsightCard(insight = insight)
            }
        }
    }
}

@Composable
private fun InsightCard(insight: Insight) {
    val (borderColor, bgAlpha) = when (insight) {
        is Insight.Warning -> BugListColors.DebtRed to 0.08f
        is Insight.Info -> BugListColors.Gold to 0.08f
        is Insight.Celebration -> BugListColors.DebtGreen to 0.08f
    }
    val message = when (insight) {
        is Insight.Warning -> insight.message
        is Insight.Info -> insight.message
        is Insight.Celebration -> insight.message
    }

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = borderColor.copy(alpha = bgAlpha)
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .width(220.dp)
    ) {
        Text(
            text = message,
            fontFamily = RobotoCondensedFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = BugListColors.Platinum,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            lineHeight = 18.sp
        )
    }
}

// ── 4. Bento Grid ───────────────────────────────────────────────────────────────

@Composable
private fun BentoGridSection(
    atRiskAmount: Double,
    repaymentRate: Double,
    avgDurationDays: Double,
    openOwedToMe: Double,
    openIOwe: Double,
    totalOpenCount: Int
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(text = stringResource(R.string.statistics_overview_title))
        Spacer(Modifier.height(8.dp))

        // At-Risk card — full width, red accent
        AtRiskCard(atRiskAmount = atRiskAmount)
        Spacer(Modifier.height(8.dp))

        // 2×2 row: Repayment Rate + Avg Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepaymentRateTile(
                rate = repaymentRate,
                modifier = Modifier.weight(1f)
            )
            AvgDurationTile(
                avgDays = avgDurationDays,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))

        // Overview row: Schuldet mir + Ich schulde + Offen count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverviewCard(
                label = stringResource(R.string.statistics_owed_to_me),
                amount = openOwedToMe,
                amountColor = BugListColors.DebtGreen,
                modifier = Modifier.weight(1f)
            )
            OverviewCard(
                label = stringResource(R.string.statistics_i_owe),
                amount = openIOwe,
                amountColor = BugListColors.DebtRed,
                modifier = Modifier.weight(1f)
            )
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
                        fontSize = 9.sp,
                        color = BugListColors.Muted,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = totalOpenCount.toString(),
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
private fun AtRiskCard(atRiskAmount: Double) {
    val hasRisk = atRiskAmount > 0.01
    val containerColor = if (hasRisk) BugListColors.DebtRed.copy(alpha = 0.12f) else BugListColors.Surface
    val borderColor = if (hasRisk) BugListColors.DebtRed else BugListColors.Divider

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.statistics_bento_at_risk),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (hasRisk) BugListColors.DebtRed else BugListColors.Muted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = stringResource(R.string.statistics_bento_at_risk_subtitle),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 11.sp,
                    color = BugListColors.Muted
                )
            }
            Text(
                text = formatAmount(atRiskAmount, "EUR"),
                fontFamily = BebasNeueFontFamily,
                fontSize = 26.sp,
                color = if (hasRisk) BugListColors.DebtRed else BugListColors.Muted
            )
        }
    }
}

@Composable
private fun RepaymentRateTile(rate: Double, modifier: Modifier = Modifier) {
    val pct = (rate * 100).roundToInt().coerceIn(0, 100)
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
                text = stringResource(R.string.statistics_bento_repayment_rate),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = BugListColors.Muted,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            // Mini donut
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 7.dp.toPx()
                    val inset = strokeW / 2f
                    val arcSize = Size(size.width - strokeW, size.height - strokeW)
                    // Background arc
                    drawArc(
                        color = BugListColors.SurfaceHigh,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )
                    // Filled arc
                    if (pct > 0) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(BugListColors.DebtGreen, BugListColors.Gold)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * pct / 100f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = "$pct%",
                    fontFamily = BebasNeueFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Platinum
                )
            }
        }
    }
}

@Composable
private fun AvgDurationTile(avgDays: Double, modifier: Modifier = Modifier) {
    val days = avgDays.roundToInt()
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
                text = stringResource(R.string.statistics_bento_avg_duration),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = BugListColors.Muted,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = days.toString(),
                fontFamily = BebasNeueFontFamily,
                fontSize = 40.sp,
                color = BugListColors.Gold
            )
            Text(
                text = stringResource(R.string.statistics_bento_avg_duration_unit),
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 11.sp,
                color = BugListColors.Muted
            )
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
                fontSize = 9.sp,
                color = BugListColors.Muted,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            val displayAmount = if (showSign) amount else abs(amount)
            Text(
                text = formatAmount(displayAmount, "EUR"),
                fontFamily = BebasNeueFontFamily,
                fontSize = 18.sp,
                color = amountColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── 5. Monthly Bar Chart ─────────────────────────────────────────────────────────

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
                            .background(BugListColors.DebtGreen, RoundedCornerShape(2.dp))
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
                            .background(BugListColors.DebtRed, RoundedCornerShape(2.dp))
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
                                    fill = fill(BugListColors.DebtGreen),
                                    thickness = 16.dp,
                                    shape = CorneredShape.rounded(
                                        topLeftPercent = 40,
                                        topRightPercent = 40
                                    )
                                ),
                                rememberLineComponent(
                                    fill = fill(BugListColors.DebtRed),
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

// ── 6. Top Persons with Reliability Score ───────────────────────────────────────

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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = "#$rank",
            fontFamily = BebasNeueFontFamily,
            fontSize = 16.sp,
            color = BugListColors.GoldDim,
            modifier = Modifier.width(28.dp)
        )
        // Reliability ring around avatar
        ReliabilityRing(
            score = item.reliabilityScore,
            size = 40.dp
        ) {
            PersonAvatar(
                name = item.person.name,
                avatarColor = item.person.avatarColor,
                size = 32.dp,
                avatarImagePath = item.person.avatarImagePath
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.person.name,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BugListColors.Platinum
            )
            Text(
                text = "${item.reliabilityScore}% ${stringResource(R.string.statistics_reliability_score_label)}",
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 10.sp,
                color = reliabilityColor(item.reliabilityScore)
            )
        }
        Text(
            text = formatAmount(item.totalAmount, "EUR"),
            fontFamily = BebasNeueFontFamily,
            fontSize = 18.sp,
            color = amountColor
        )
    }
}

/**
 * A small circular reliability ring drawn with Canvas.
 * Score 0-40 = red, 41-70 = orange, 71-100 = green.
 */
@Composable
private fun ReliabilityRing(
    score: Int,
    size: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 3.dp.toPx()
            val inset = strokeW / 2f
            val arcSize = Size(this.size.width - strokeW, this.size.height - strokeW)
            // Background track
            drawArc(
                color = BugListColors.SurfaceHigh,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeW, cap = StrokeCap.Round)
            )
            // Score arc
            if (score > 0) {
                drawArc(
                    color = reliabilityColorValue(score),
                    startAngle = -90f,
                    sweepAngle = 360f * score / 100f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeW, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

private fun reliabilityColorValue(score: Int): Color = when {
    score <= 40 -> BugListColors.DebtRed
    score <= 70 -> BugListColors.Orange
    else -> BugListColors.DebtGreen
}

@Composable
private fun reliabilityColor(score: Int): Color = reliabilityColorValue(score)

// ── 7. Activity Heatmap ─────────────────────────────────────────────────────────

@Composable
private fun ActivityHeatmapSection(activityData: Map<Long, DayActivityCount>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionHeader(
                text = stringResource(R.string.statistics_heatmap_title),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.statistics_heatmap_subtitle),
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 11.sp,
                color = BugListColors.Muted
            )
        }
        Spacer(Modifier.height(10.dp))
        ActivityHeatmapGrid(activityData = activityData)
    }
}

@Composable
private fun ActivityHeatmapGrid(activityData: Map<Long, DayActivityCount>) {
    val cellSize = 10.dp
    val gapSize = 2.dp
    val weeks = 13
    val days = 7

    // Find max total for intensity normalization
    val maxTotal = activityData.values.maxOfOrNull { it.total } ?: 1

    // Build a grid: week columns × day rows
    // dayIndex 0 = oldest day, 90 = today
    val nowMs = System.currentTimeMillis()
    val oneDayMs = 86_400_000L

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BugListColors.Surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gapSize)) {
                for (week in 0 until weeks) {
                    Column(verticalArrangement = Arrangement.spacedBy(gapSize)) {
                        for (day in 0 until days) {
                            val dayIndex = (weeks - 1 - week) * 7 + (days - 1 - day)
                            val dayStart = ((nowMs / oneDayMs) - dayIndex) * oneDayMs
                            val activity = activityData[dayStart]
                            val intensity = if (activity != null && maxTotal > 0) {
                                activity.total.toFloat() / maxTotal.toFloat()
                            } else 0f

                            val cellColor = when {
                                activity == null || activity.total == 0 ->
                                    BugListColors.SurfaceHigh
                                activity.paymentCount > activity.newDebtCount ->
                                    // Payment day: gold
                                    BugListColors.Gold.copy(alpha = 0.15f + 0.70f * intensity)
                                else ->
                                    // New debt day: slightly reddish
                                    BugListColors.DebtRed.copy(alpha = 0.12f + 0.60f * intensity)
                            }

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }
            // Legend
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(1.dp)).background(BugListColors.Gold.copy(alpha = 0.7f)))
                Spacer(Modifier.width(4.dp))
                Text("Zahlung", fontFamily = RobotoCondensedFontFamily, fontSize = 9.sp, color = BugListColors.Muted)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(1.dp)).background(BugListColors.DebtRed.copy(alpha = 0.6f)))
                Spacer(Modifier.width(4.dp))
                Text("Neue Schuld", fontFamily = RobotoCondensedFontFamily, fontSize = 9.sp, color = BugListColors.Muted)
            }
        }
    }
}

// ── 8. Status Distribution ───────────────────────────────────────────────────────

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

// ── 9. Paid History ──────────────────────────────────────────────────────────────

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

// ── 10. Recent Activity Timeline ─────────────────────────────────────────────────

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
    val amountColor = if (entry.isOwedToMe) BugListColors.DebtGreen else BugListColors.DebtRed

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

// ── Shared UI helpers ─────────────────────────────────────────────────────────────

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
