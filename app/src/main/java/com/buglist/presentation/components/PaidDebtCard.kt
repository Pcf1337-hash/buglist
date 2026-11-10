package com.buglist.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.debtSerial
import com.buglist.domain.model.paidAt
import com.buglist.domain.model.paymentProgress
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import com.buglist.presentation.theme.LocalAmbientShift
import com.buglist.presentation.theme.ambientRim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaidDebtCard(
    debt: DebtEntryWithPayments,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val entry = debt.entry
    val date = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    val timestamp = SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMAN)
    val progress = debt.paymentProgress()
    Surface(
        modifier = modifier.fillMaxWidth().ambientRim(LocalAmbientShift.current).animateContentSize()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = BugListColors.SurfaceCard,
        border = BorderStroke(1.dp, BugListColors.DebtGreen.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = BugListColors.DebtGreen)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.paid_complete), color = BugListColors.DebtGreen,
                        fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(debt.paidAt?.let { timestamp.format(Date(it)) }
                        ?: stringResource(R.string.paid_date_unknown),
                        color = BugListColors.TextPrimary, fontFamily = RobotoCondensedFontFamily, fontSize = 14.sp)
                }
            }
            if (!entry.description.isNullOrBlank()) {
                Text(entry.description, color = BugListColors.TextPrimary,
                    fontFamily = RobotoCondensedFontFamily, fontWeight = FontWeight.Medium, fontSize = 17.sp)
            }
            if (entry.tags.isNotEmpty()) {
                Text(entry.tags.joinToString(" · "), color = BugListColors.Gold,
                    fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp)
            }
            Text(stringResource(R.string.paid_original_date, date.format(Date(entry.date))),
                color = BugListColors.TextSecondary, fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp)
            Text(stringResource(if (entry.isOwedToMe) R.string.paid_received else R.string.paid_sent),
                color = BugListColors.TextSecondary, fontFamily = OswaldFontFamily, fontSize = 12.sp)
            Text(formatAmount(entry.amount, entry.currency), color = BugListColors.TextPrimary,
                fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(if (progress.isEmpty()) stringResource(R.string.paid_no_records)
                else pluralStringResource(R.plurals.paid_payment_count, progress.size, progress.size),
                color = BugListColors.TextSecondary, fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp)
            if (debt.paidAt == null && progress.isNotEmpty()) {
                Text(stringResource(R.string.paid_incomplete_records), color = BugListColors.TextSecondary,
                    fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp)
            }
            Text(stringResource(if (expanded) R.string.paid_hide_payments else R.string.paid_show_payments),
                color = BugListColors.Gold, fontFamily = RobotoCondensedFontFamily, fontSize = 14.sp)
            if (expanded) {
                HorizontalDivider(color = BugListColors.Divider)
                Text(stringResource(R.string.debt_card_payment_history), color = BugListColors.Gold,
                    fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                progress.forEachIndexed { index, step ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.paid_payment_number, index + 1,
                            if (step.payment.date > 0) timestamp.format(Date(step.payment.date))
                            else stringResource(R.string.paid_date_unknown)),
                            color = BugListColors.TextSecondary, fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp)
                        Text(formatAmount(step.payment.amount, entry.currency), color = BugListColors.TextPrimary,
                            fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (!step.payment.note.isNullOrBlank()) {
                            Text(step.payment.note, color = BugListColors.TextSecondary,
                                fontFamily = RobotoCondensedFontFamily, fontSize = 13.sp)
                        }
                        Text(if (step.completesDebt) stringResource(R.string.paid_final_payment)
                            else stringResource(R.string.paid_remaining_after, formatAmount(step.remaining, entry.currency)),
                            color = if (step.completesDebt) BugListColors.DebtGreen else BugListColors.TextSecondary,
                            fontFamily = RobotoCondensedFontFamily, fontSize = 14.sp)
                    }
                }
                if (progress.isEmpty()) {
                    Text(stringResource(R.string.paid_no_records), color = BugListColors.TextSecondary,
                        fontFamily = RobotoCondensedFontFamily, fontSize = 14.sp)
                }
                Text(debtSerial(entry.createdAt, entry.id, entry.syncSerial), color = BugListColors.TextMuted,
                    fontFamily = RobotoCondensedFontFamily, fontSize = 11.sp)
            }
        }
    }
}
