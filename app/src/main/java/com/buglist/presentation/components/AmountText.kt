package com.buglist.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.buglist.presentation.theme.BebasNeueFontFamily
import com.buglist.presentation.theme.BugListColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Amount display text using Bebas Neue font.
 *
 * Color is automatically assigned:
 * - Positive amount → [BugListColors.DebtGreen]
 * - Negative amount → [BugListColors.DebtRed]
 * - Zero → [BugListColors.Muted]
 *
 * @param amount       The monetary value (positive = owed to user, negative = user owes).
 * @param currency     ISO 4217 currency code (default EUR).
 * @param fontSize     Font size. Defaults to 24sp.
 * @param modifier     Optional layout modifier.
 * @param forceColor   Override automatic color selection.
 */
@Composable
fun AmountText(
    amount: Double,
    currency: String = "EUR",
    fontSize: TextUnit = 24.sp,
    modifier: Modifier = Modifier,
    forceColor: Color? = null
) {
    val color = forceColor ?: when {
        amount > 0.001 -> BugListColors.DebtGreen
        amount < -0.001 -> BugListColors.DebtRed
        else -> BugListColors.Muted
    }

    val formatted = formatAmount(abs(amount), currency)
    val prefix = when {
        amount < -0.001 -> "- "
        amount > 0.001 -> "+ "
        else -> ""
    }

    Text(
        text = "$prefix$formatted",
        fontFamily = BebasNeueFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}

/**
 * Formats an amount for display.
 * Uses German locale style for EUR (e.g., "1.234,56 €"), US style for USD/others.
 */
fun formatAmount(amount: Double, currency: String): String {
    val locale = if (currency == "EUR") Locale.GERMAN else Locale.US
    val format = NumberFormat.getCurrencyInstance(locale)
    format.currency = java.util.Currency.getInstance(currency)
    return format.format(amount)
}
