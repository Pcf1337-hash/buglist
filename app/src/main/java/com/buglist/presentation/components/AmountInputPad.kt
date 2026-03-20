package com.buglist.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.presentation.theme.BebasNeueFontFamily
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily

/**
 * Einhand-optimierter Betragseingabe-Block.
 *
 * Layout (von oben nach unten):
 * 1. Display — aktueller Betrag in Bebas Neue Gold, groß
 * 2. Schnell-Minus-Zeile: -100 / -50 / -20 / -10 (rot)
 * 3. Numpad: 7-8-9 / 4-5-6 / 1-2-3 / ,-0-⌫
 * 4. Schnell-Plus-Zeile: +10 / +20 / +50 / +100 (grün)
 *
 * Validierungsregeln (see L-036):
 * - Max. ein Dezimalkomma
 * - Max. 2 Nachkommastellen
 * - Maximalwert: 999.999,99
 * - Schnell-Buttons subtrahieren min. bis 0 (nie negativ)
 *
 * @param inputString   Current raw input string (e.g., "123,45"). State hoisted by caller.
 * @param onInputChange Called with the new input string after every keystroke or button.
 * @param maxValue      Maximum allowed value. Defaults to 999999.99.
 * @param currency      Currency symbol to show in display (default EUR = "€").
 */
@Composable
fun AmountInputPad(
    inputString: String,
    onInputChange: (String) -> Unit,
    maxValue: Double = 999_999.99,
    currency: String = "EUR",
    modifier: Modifier = Modifier
) {
    val currencySymbol = when (currency) {
        "USD" -> "$"
        "GBP" -> "£"
        "CHF" -> "CHF"
        "JPY" -> "¥"
        else -> "€"
    }

    Column(
        modifier = modifier
            .background(BugListColors.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Display
        Text(
            text = "$currencySymbol $inputString",
            fontFamily = BebasNeueFontFamily,
            fontSize = 48.sp,
            color = BugListColors.Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Quick-Minus row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(-100, -50, -20, -10).forEach { delta ->
                QuickButton(
                    label = delta.toString(),
                    color = BugListColors.DebtRed,
                    onClick = {
                        val current = inputString.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val newVal = maxOf(0.0, current + delta)
                        onInputChange(formatForInput(newVal))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Numpad
        val rows = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf(",", "0", "⌫")
        )
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                row.forEach { key ->
                    if (key == "⌫") {
                        BackspaceButton(
                            onTap = {
                                val newInput = if (inputString.isNotEmpty())
                                    inputString.dropLast(1)
                                else ""
                                onInputChange(newInput)
                            },
                            onLongPress = { onInputChange("") },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        NumpadKey(
                            label = key,
                            onClick = {
                                val newInput = appendInput(inputString, key, maxValue)
                                onInputChange(newInput)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Quick-Plus row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(10, 20, 50, 100).forEach { delta ->
                QuickButton(
                    label = "+$delta",
                    color = BugListColors.DebtGreen,
                    onClick = {
                        val current = inputString.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val newVal = minOf(maxValue, current + delta)
                        onInputChange(formatForInput(newVal))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Appends a digit or comma to the input string with validation.
 * See L-036 in lessons.md.
 *
 * Edge cases handled:
 * - Only one comma allowed (L-036)
 * - Max 2 decimal places after comma (L-036)
 * - Max value enforced
 * - Leading zero: "0" followed by a digit replaces the zero (e.g. "0" + "5" → "5")
 * - Comma after empty/zero → "0," prefix
 */
internal fun appendInput(current: String, digit: String, maxValue: Double): String {
    // Guard: max one comma (L-036)
    if (digit == "," && current.contains(",")) return current

    // Guard: max 2 decimal places after comma (L-036)
    if (current.contains(",")) {
        val decimalPart = current.substringAfter(",")
        if (decimalPart.length >= 2 && digit != ",") return current
    }

    // Leading-zero guard: "0" + digit (not comma) → replace with digit
    if (current == "0" && digit != ",") {
        val candidate = digit
        val numericValue = candidate.replace(",", ".").toDoubleOrNull() ?: return current
        if (numericValue > maxValue) return current
        return candidate
    }

    // Comma after empty input → start with "0,"
    val base = if (current.isEmpty() && digit == ",") "0" else current
    val candidate = base + digit

    // Guard: validate against max value
    val numericValue = candidate.replace(",", ".").toDoubleOrNull() ?: return current
    if (numericValue > maxValue) return current

    return candidate
}

/**
 * Formats a Double for display in the input pad (German locale: comma as decimal separator).
 */
internal fun formatForInput(value: Double): String {
    if (value == 0.0) return ""
    return if (value == kotlin.math.floor(value)) {
        value.toLong().toString()
    } else {
        // Format with up to 2 decimal places, using comma
        String.format(java.util.Locale.ROOT, "%.2f", value).replace(".", ",").trimEnd('0').trimEnd(',')
    }
}

@Composable
private fun NumpadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = BugListColors.SurfaceHigh,
        modifier = modifier.height(56.dp)
    ) {
        Text(
            text = label,
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BugListColors.Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        )
    }
}

@Composable
private fun BackspaceButton(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    // rememberUpdatedState ensures the pointerInput block (which never restarts because
    // its key is Unit) always calls the LATEST lambda — not the one captured at first
    // composition. Without this, every tap after the first re-uses the stale inputString
    // closure and the delete appears to stop working after one character. (L-037)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BugListColors.SurfaceHigh,
        modifier = modifier
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onLongPress = { currentOnLongPress() }
                )
            }
    ) {
        Text(
            text = "⌫",
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BugListColors.Platinum,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        )
    }
}

@Composable
private fun QuickButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier.height(40.dp)
    ) {
        Text(
            text = label,
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            letterSpacing = 0.5.sp
        )
    }
}
