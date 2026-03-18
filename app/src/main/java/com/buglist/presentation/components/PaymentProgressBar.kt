package com.buglist.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.buglist.presentation.theme.BugListColors

/**
 * Horizontal progress bar showing how much of a debt has been paid.
 *
 * Gold fill = amount paid. Dark gray = remaining.
 * Visible only when [totalPaid] > 0 (PARTIAL or PAID states).
 *
 * @param totalPaid   Amount already paid.
 * @param totalAmount Original debt amount.
 * @param modifier    Optional layout modifier.
 */
@Composable
fun PaymentProgressBar(
    totalPaid: Double,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val progress = if (totalAmount > 0.0) {
        (totalPaid / totalAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = BugListColors.Gold,
        trackColor = BugListColors.Divider,
    )
}
