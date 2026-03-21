package com.buglist.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Payment
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card displaying a single debt entry with payment progress.
 *
 * Border color:
 * - OPEN    → Gold
 * - PARTIAL → Orange
 * - PAID    → Transparent (no border)
 * - CANCELLED → Muted
 *
 * Expandable: when [expanded] is true, shows the full payment history.
 *
 * @param debtWithPayments  The debt entry and its payment records.
 * @param expanded          Whether the payment history is visible.
 * @param onClick           Called when the card is tapped (toggles expansion).
 * @param modifier          Optional layout modifier.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebtCard(
    debtWithPayments: DebtEntryWithPayments,
    expanded: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val entry = debtWithPayments.entry
    val borderColor = when (entry.status) {
        DebtStatus.OPEN -> BugListColors.Gold
        DebtStatus.PARTIAL -> BugListColors.Orange
        DebtStatus.PAID -> Color.Transparent
        DebtStatus.CANCELLED -> BugListColors.Muted
    }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BugListColors.Surface,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: amount + status chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val displayAmount = if (entry.status in listOf(DebtStatus.PAID, DebtStatus.CANCELLED)) {
                    entry.amount
                } else {
                    debtWithPayments.remaining
                }
                AmountText(
                    amount = if (entry.isOwedToMe) displayAmount else -displayAmount,
                    currency = entry.currency,
                    fontSize = 28.sp
                )
                StatusChip(status = entry.status)
            }

            // Description
            if (!entry.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.description,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Platinum,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Tags — shown as small inline chips when present
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.tags.forEach { tagName ->
                        Surface(
                            color = BugListColors.SurfaceHigh,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = tagName,
                                fontFamily = RobotoCondensedFontFamily,
                                fontSize = 11.sp,
                                color = BugListColors.Gold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Date row
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dateFormat.format(Date(entry.date)),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 12.sp,
                    color = BugListColors.Muted
                )
                if (entry.dueDate != null) {
                    Text(
                        text = "Due: ${dateFormat.format(Date(entry.dueDate))}",
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 12.sp,
                        color = BugListColors.Muted
                    )
                }
            }

            // PARTIAL progress info
            if (entry.status == DebtStatus.PARTIAL && debtWithPayments.totalPaid > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.debt_card_paid_info,
                        formatAmount(debtWithPayments.totalPaid, entry.currency),
                        formatAmount(entry.amount, entry.currency),
                        formatAmount(debtWithPayments.remaining, entry.currency)
                    ),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 12.sp,
                    color = BugListColors.Muted
                )
                Spacer(Modifier.height(6.dp))
                PaymentProgressBar(
                    totalPaid = debtWithPayments.totalPaid,
                    totalAmount = entry.amount
                )
            } else if (entry.status == DebtStatus.PAID) {
                Spacer(Modifier.height(6.dp))
                PaymentProgressBar(
                    totalPaid = entry.amount,
                    totalAmount = entry.amount
                )
            }

            // Expandable payment history
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = BugListColors.Divider)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.debt_card_payment_history),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BugListColors.Gold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                if (debtWithPayments.payments.isEmpty()) {
                    Text(
                        text = stringResource(R.string.debt_card_no_payments),
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 13.sp,
                        color = BugListColors.Muted
                    )
                } else {
                    debtWithPayments.payments.forEach { payment ->
                        PaymentHistoryRow(
                            payment = payment,
                            currency = entry.currency,
                            isOwedToMe = entry.isOwedToMe
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryRow(payment: Payment, currency: String, isOwedToMe: Boolean) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    // isOwedToMe=true  → green debt  → payment reduces what they owe me  → negative (red)
    // isOwedToMe=false → red debt   → payment reduces what I owe        → positive (green)
    val signedAmount = if (isOwedToMe) -payment.amount else payment.amount
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = dateFormat.format(Date(payment.date)),
            fontFamily = RobotoCondensedFontFamily,
            fontSize = 13.sp,
            color = BugListColors.Muted
        )
        if (!payment.note.isNullOrBlank()) {
            Text(
                text = payment.note,
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 13.sp,
                color = BugListColors.Muted,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }
        AmountText(
            amount = signedAmount,
            currency = currency,
            fontSize = 13.sp
        )
    }
}
