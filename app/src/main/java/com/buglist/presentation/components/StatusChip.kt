package com.buglist.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.domain.model.DebtStatus
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.RobotoCondensedFontFamily

/**
 * Status badge for a [DebtStatus] value.
 *
 * Visual style per status:
 * - OPEN     → Gold outline, transparent background
 * - PARTIAL  → Orange filled
 * - PAID     → DebtGreen filled
 * - CANCELLED→ Muted filled
 */
@Composable
fun StatusChip(
    status: DebtStatus,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, borderColor) = when (status) {
        DebtStatus.OPEN -> Triple(
            Color.Transparent,
            BugListColors.Gold,
            BugListColors.Gold
        )
        DebtStatus.PARTIAL -> Triple(
            BugListColors.Orange,
            BugListColors.Background,
            BugListColors.Orange
        )
        DebtStatus.PAID -> Triple(
            BugListColors.DebtGreen,
            BugListColors.Background,
            BugListColors.DebtGreen
        )
        DebtStatus.CANCELLED -> Triple(
            BugListColors.Muted,
            BugListColors.Background,
            BugListColors.Muted
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        modifier = modifier
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
    ) {
        val label = when (status) {
            DebtStatus.OPEN -> stringResource(R.string.status_open)
            DebtStatus.PARTIAL -> stringResource(R.string.status_partial)
            DebtStatus.PAID -> stringResource(R.string.status_paid)
            DebtStatus.CANCELLED -> stringResource(R.string.status_cancelled)
        }
        Text(
            text = label,
            fontFamily = RobotoCondensedFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = contentColor,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
