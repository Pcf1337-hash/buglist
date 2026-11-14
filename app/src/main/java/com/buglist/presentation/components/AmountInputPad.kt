package com.buglist.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.presentation.theme.BebasNeueFontFamily
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Einhand-optimierter Betragseingabe-Block.
 *
 * Layout (von oben nach unten):
 * 1. Display — aktueller Betrag in Bebas Neue Gold, groß
 * 2. Numpad: 7-8-9 / 4-5-6 / 1-2-3 / ,-0-⌫
 *
 * Validierungsregeln (see L-036):
 * - Max. ein Dezimalkomma
 * - Max. 2 Nachkommastellen
 * - Maximalwert: 999.999,99
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
    modifier: Modifier = Modifier,
    keyHeight: Dp = 64.dp,
    keyGap: Dp = 8.dp,
    belowDisplay: @Composable () -> Unit = {}
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
        // Display — tabular figures prevent sideways jitter during input (L-093)
        Text(
            text = "$currencySymbol $inputString",
            fontFamily = BebasNeueFontFamily,
            fontSize = 52.sp,
            color = BugListColors.Gold,
            textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        belowDisplay()

        Spacer(Modifier.height(keyGap))

        // Numpad
        val rows = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf(",", "0", "⌫")
        )
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(keyGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = keyGap)
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
                            modifier = Modifier.weight(1f).height(keyHeight)
                        )
                    } else {
                        NumpadKey(
                            label = key,
                            onClick = {
                                val newInput = appendInput(inputString, key, maxValue)
                                onInputChange(newInput)
                            },
                            modifier = Modifier.weight(1f).height(keyHeight)
                        )
                    }
                }
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

@Composable
private fun NumpadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val feedback = keyPressFeedback(interactionSource)
    Surface(
        onClick = { feedback.pulse(); onClick() },
        interactionSource = interactionSource,
        shape = shape,
        color = BugListColors.SurfaceHigh,
        modifier = modifier.then(feedback.modifier).border(1.5.dp, BugListColors.Gold, shape)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                maxLines = 1,
                color = BugListColors.Gold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun BackspaceButton(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deleteLabel = stringResource(R.string.amount_delete_digit)
    val clearLabel = stringResource(R.string.amount_clear)
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val feedback = keyPressFeedback(interactionSource)
    Surface(
        shape = shape,
        color = BugListColors.SurfaceHigh,
        modifier = modifier
            .then(feedback.modifier)
            .border(1.5.dp, BugListColors.Gold, shape)
            .semantics { contentDescription = deleteLabel }
            .combinedClickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = { feedback.pulse(); onTap() },
                onLongClickLabel = clearLabel,
                onLongClick = { feedback.pulse(); onLongPress() }
            )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "⌫",
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                maxLines = 1,
                color = BugListColors.Platinum,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

private val KeyHaloStrokes = listOf(14f to 0.10f, 8f to 0.24f, 4f to 0.55f)

private class KeyFeedback(val modifier: Modifier, val pulse: () -> Unit)

/** Every accepted click flashes, including the first tap and accessibility activation. */
@Composable
private fun keyPressFeedback(interactionSource: MutableInteractionSource): KeyFeedback {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val pulseJob = remember { mutableStateOf<Job?>(null) }
    val pulse: () -> Unit = {
        pulseJob.value?.cancel()
        pulseJob.value = scope.launch {
            press.snapTo(1f)
            delay(90)
            press.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 280f))
        }
    }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pulseJob.value?.cancel()
                    press.animateTo(1f, tween(65))
                }
                is PressInteraction.Release -> {
                    if (pulseJob.value?.isActive == true) return@collectLatest
                    // Keep release feedback for holds and cancelled click gestures.
                    if (press.value < 1f) press.animateTo(1f, tween(45))
                    press.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 280f))
                }
                is PressInteraction.Cancel -> {
                    pulseJob.value?.cancel()
                    press.animateTo(0f, tween(80))
                }
            }
        }
    }
    val feedbackModifier = Modifier
        .graphicsLayer {
            val scale = 1f - 0.085f * press.value
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            val glow = press.value.coerceIn(0f, 1f)
            if (glow > 0f) {
                // Layered strokes give a visible LED halo without an offscreen blur.
                KeyHaloStrokes.forEach { (width, alpha) ->
                    drawRoundRect(BugListColors.Gold.copy(alpha = alpha * glow),
                        cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(width.dp.toPx()))
                }
            }
            drawContent()
            if (glow > 0f) {
                val corner = CornerRadius(14.dp.toPx())
                drawRoundRect(BugListColors.Gold.copy(alpha = 0.28f * glow), cornerRadius = corner)
                drawRoundRect(Color.White.copy(alpha = 0.95f * glow), cornerRadius = corner,
                    style = Stroke(3.dp.toPx()))
            }
        }
    return KeyFeedback(feedbackModifier, pulse)
}
