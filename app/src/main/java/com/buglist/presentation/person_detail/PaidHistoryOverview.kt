package com.buglist.presentation.person_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.SyncRole
import com.buglist.presentation.components.formatAmount
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily

@Composable
internal fun PaidHistoryHeader(name: String, count: Int, role: SyncRole?, onCopy: () -> Unit, onSecretTap: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(name.uppercase(), modifier = Modifier.clickable {
            val now = System.currentTimeMillis()
            taps = if (now - lastTap > 2_000) 1 else taps + 1
            lastTap = now
            if (taps >= 10) { taps = 0; onSecretTap() }
        }, color = BugListColors.TextPrimary, fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp)
        if (role == SyncRole.GUEST) {
            Text(stringResource(R.string.couple_badge_view), color = BugListColors.Gold,
                fontFamily = OswaldFontFamily, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pluralStringResource(R.plurals.paid_debt_count, count, count), modifier = Modifier.weight(1f),
                color = BugListColors.DebtGreen, fontFamily = RobotoCondensedFontFamily, fontSize = 15.sp, lineHeight = 20.sp)
            TextButton(onClick = onCopy) {
                Text(stringResource(R.string.paid_copy_list), color = BugListColors.Gold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun PaidHistoryOverview(debts: List<DebtEntryWithPayments>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.paid_sort_hint), color = BugListColors.TextSecondary,
            fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp, lineHeight = 18.sp)
        debts.groupBy { it.entry.currency }.toSortedMap().forEach { (currency, entries) ->
            // Never net opposite directions or combine different currencies.
            Surface(shape = RoundedCornerShape(16.dp), color = BugListColors.SurfaceElevated) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.paid_received) + ": " + formatAmount(
                        entries.filter { it.entry.isOwedToMe }.sumOf { it.entry.amount }, currency),
                        color = BugListColors.TextPrimary, fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp)
                    Text(stringResource(R.string.paid_sent) + ": " + formatAmount(
                        entries.filter { !it.entry.isOwedToMe }.sumOf { it.entry.amount }, currency),
                        color = BugListColors.TextPrimary, fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp)
                }
            }
        }
        Text(stringResource(R.string.paid_fifo_hint), color = BugListColors.TextSecondary,
            fontFamily = RobotoCondensedFontFamily, fontSize = 12.sp, lineHeight = 16.sp)
        Text(stringResource(R.string.paid_cancelled_hint), color = BugListColors.TextSecondary,
            fontFamily = RobotoCondensedFontFamily, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
internal fun PaidHistoryDayHeading(day: String?) {
    Text(if (day != null) stringResource(R.string.paid_day_heading, day)
        else stringResource(R.string.paid_date_unknown),
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        color = BugListColors.TextPrimary, fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold, fontSize = 16.sp)
}
