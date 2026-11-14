package com.buglist.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.domain.model.RepaymentHistoryItem
import com.buglist.domain.model.debtSerial
import com.buglist.domain.model.paymentProgress
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RepaymentCard(
    item: RepaymentHistoryItem,
    expanded: Boolean,
    onAmountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entry = item.debt.entry
    val date = SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMAN)
    val title = stringResource(if (item.payment == null) R.string.repayment_legacy
        else if (entry.isOwedToMe) R.string.repayment_received else R.string.repayment_sent)
    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        color = BugListColors.SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BugListColors.BorderSubtle)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontFamily = RobotoCondensedFontFamily, fontSize = 14.sp,
                        color = BugListColors.TextSecondary)
                    Text(item.date?.let { date.format(Date(it)) }
                        ?: stringResource(R.string.paid_date_unknown),
                        fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp,
                        color = BugListColors.TextSecondary)
                }
                Text(
                    formatAmount(item.amount, entry.currency),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = if (entry.isOwedToMe) BugListColors.DebtGreen else BugListColors.TextPrimary,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(
                            onClickLabel = stringResource(if (expanded) R.string.paid_hide_payments else R.string.paid_show_payments),
                            onClick = onAmountClick
                        )
                        .padding(start = 12.dp, top = 3.dp, bottom = 3.dp)
                )
            }
            if (expanded) {
                HorizontalDivider(color = BugListColors.Divider)
                if (!entry.description.isNullOrBlank()) {
                    Text(entry.description, fontFamily = RobotoCondensedFontFamily,
                        color = BugListColors.TextPrimary, fontSize = 15.sp)
                }
                Text(stringResource(R.string.paid_original_date, date.format(Date(entry.date))),
                    fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp,
                    color = BugListColors.TextSecondary)
                Text(debtSerial(entry.createdAt, entry.id, entry.syncSerial),
                    fontFamily = RobotoCondensedFontFamily, fontSize = 12.sp,
                    color = BugListColors.TextMuted)
                item.debt.paymentProgress().forEach { step ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (step.payment.date > 0) date.format(Date(step.payment.date))
                            else stringResource(R.string.paid_date_unknown),
                            modifier = Modifier.weight(1f),
                            fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp,
                            color = BugListColors.TextSecondary)
                        Text(formatAmount(step.payment.amount, entry.currency),
                            fontFamily = OswaldFontFamily, fontSize = 17.sp,
                            fontWeight = if (step.payment.id == item.payment?.id) FontWeight.Bold else FontWeight.Normal,
                            color = BugListColors.TextPrimary)
                    }
                    if (!step.payment.note.isNullOrBlank()) {
                        Text(step.payment.note, fontFamily = RobotoCondensedFontFamily,
                            fontSize = 13.sp, color = BugListColors.TextSecondary)
                    }
                }
                if (item.payment == null) {
                    Text(stringResource(R.string.paid_no_records), fontSize = 13.sp,
                        fontFamily = RobotoCondensedFontFamily, color = BugListColors.TextSecondary)
                }
            }
        }
    }
}
