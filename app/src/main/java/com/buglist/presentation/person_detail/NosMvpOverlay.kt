package com.buglist.presentation.person_detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.domain.model.RepaymentSummary
import com.buglist.presentation.components.formatAmount
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val KissPink = Color(0xFFFF6FA7)

/** Short, touch-through reward. Money is already committed before this appears. */
@Composable
internal fun NosMvpOverlay(summaries: List<RepaymentSummary>, onFinished: () -> Unit) {
    val arrival = remember { Animatable(0f) }
    val timeline = remember { Animatable(0f) }
    val heart = remember { heartPath() }
    LaunchedEffect(Unit) {
        launch { arrival.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 230f)) }
        timeline.animateTo(1f, tween(2800, easing = LinearEasing))
        onFinished()
    }
    Box(Modifier.fillMaxSize().graphicsLayer {
        val t = timeline.value
        alpha = when {
            t < 0.08f -> t / 0.08f
            t > 0.84f -> (1f - t) / 0.16f
            else -> 1f
        }.coerceIn(0f, 1f)
    }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(360.dp)) {
            val t = timeline.value
            val spread = (t * 1.8f).coerceIn(0f, 1f)
            drawCircle(Brush.radialGradient(listOf(KissPink.copy(alpha = 0.14f), Color.Transparent)),
                radius = size.minDimension / 2)
            repeat(18) { i ->
                val angle = i * (2f * PI.toFloat() / 18f) - PI.toFloat() / 2
                val distance = (54f + spread * (100f + (i % 3) * 14f)).dp.toPx()
                val point = center + Offset(cos(angle) * distance, sin(angle) * distance - 18.dp.toPx() * t)
                val opacity = ((1f - t) * 1.6f).coerceIn(0f, 1f)
                if (i % 3 == 0) {
                    val heartSize = 10.dp.toPx()
                    withTransform({
                        translate(point.x - heartSize / 2, point.y - heartSize / 2)
                        scale(heartSize, heartSize, pivot = Offset.Zero)
                    }) { drawPath(heart, KissPink.copy(alpha = opacity)) }
                } else {
                    val r = (if (i % 2 == 0) 3f else 1.5f).dp.toPx()
                    drawLine(BugListColors.Gold.copy(alpha = opacity), point - Offset(r, 0f), point + Offset(r, 0f), 1.dp.toPx(), StrokeCap.Round)
                    drawLine(BugListColors.Gold.copy(alpha = opacity), point - Offset(0f, r), point + Offset(0f, r), 1.dp.toPx(), StrokeCap.Round)
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BugListColors.SurfaceCard,
            border = BorderStroke(1.dp, BugListColors.Gold.copy(alpha = 0.4f)),
            modifier = Modifier.width(280.dp).graphicsLayer {
                scaleX = 0.78f + 0.22f * arrival.value
                scaleY = scaleX
                translationY = (1f - arrival.value) * 28.dp.toPx() - timeline.value * 6.dp.toPx()
                rotationZ = (1f - arrival.value) * -5f
            }
        ) {
            Column(
                Modifier.background(Brush.verticalGradient(listOf(Color(0xFF20304B), BugListColors.SurfaceCard)))
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.nos_mvp_title), fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 4.sp,
                    color = BugListColors.Gold)
                KissSeal(Modifier.size(112.dp).graphicsLayer {
                    rotationZ = -8f + 5f * arrival.value
                    scaleX = 0.85f + 0.15f * arrival.value
                    scaleY = scaleX
                })
                summaries.forEach { summary ->
                    Text(stringResource(R.string.nos_mvp_total), fontFamily = RobotoCondensedFontFamily,
                        fontSize = 11.sp, letterSpacing = 2.sp, color = BugListColors.TextSecondary)
                    Text(formatAmount(summary.repaid, summary.currency), fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Color.White)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.nos_mvp_rate), fontFamily = RobotoCondensedFontFamily,
                            fontSize = 13.sp, color = BugListColors.TextSecondary)
                        Text(String.format(Locale.GERMAN, "%.1f %%", summary.fraction * 100f),
                            fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, color = BugListColors.Gold)
                    }
                    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                        drawRoundRect(BugListColors.SurfaceOverlay, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height))
                        val fill = size.width * summary.fraction * arrival.value.coerceIn(0f, 1f)
                        if (fill > 0f) drawRoundRect(Brush.horizontalGradient(listOf(KissPink, BugListColors.Gold)),
                            size = androidx.compose.ui.geometry.Size(fill, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.nos_mvp_kiss), fontFamily = RobotoCondensedFontFamily,
                    fontSize = 13.sp, color = KissPink)
            }
        }
    }
}

@Composable
private fun KissSeal(modifier: Modifier = Modifier) {
    val upper = remember { Path().apply {
        moveTo(6f, 50f); cubicTo(23f, 43f, 32f, 22f, 45f, 34f)
        cubicTo(49f, 39f, 51f, 39f, 55f, 34f); cubicTo(68f, 22f, 77f, 43f, 94f, 50f)
        cubicTo(75f, 53f, 62f, 55f, 50f, 52f); cubicTo(36f, 56f, 21f, 54f, 6f, 50f); close()
    } }
    val lower = remember { Path().apply {
        moveTo(6f, 50f); cubicTo(23f, 64f, 28f, 78f, 50f, 80f)
        cubicTo(72f, 78f, 77f, 64f, 94f, 50f); cubicTo(73f, 58f, 63f, 56f, 50f, 57f)
        cubicTo(36f, 56f, 26f, 58f, 6f, 50f); close()
    } }
    val highlight = remember { Path().apply {
        moveTo(28f, 42f); cubicTo(35f, 37f, 39f, 33f, 43f, 37f)
        moveTo(38f, 70f); cubicTo(47f, 74f, 59f, 72f, 65f, 67f)
    } }
    Canvas(modifier) {
        val radius = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(KissPink.copy(alpha = 0.16f), Color(0xFF111B2D))), radius = radius)
        drawCircle(KissPink.copy(alpha = 0.35f), radius = radius - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
        drawArc(BugListColors.Gold, -70f, 52f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        withTransform({
            translate(size.width * 0.11f, size.height * 0.03f)
            scale(size.width * 0.78f / 100f, size.height * 0.78f / 100f, pivot = Offset.Zero)
        }) {
            drawPath(upper, Brush.verticalGradient(listOf(Color(0xFFFFC4D8), KissPink, Color(0xFFB42C66)), startY = 30f, endY = 58f))
            drawPath(lower, Brush.verticalGradient(listOf(Color(0xFFB42C66), KissPink, Color(0xFFFFC4D8)), startY = 50f, endY = 80f))
            drawPath(highlight, Color.White.copy(alpha = 0.7f), style = Stroke(1.5f, cap = StrokeCap.Round))
        }
    }
}

private fun heartPath() = Path().apply {
    moveTo(0.5f, 0.95f)
    cubicTo(0.35f, 0.8f, 0f, 0.57f, 0f, 0.3f)
    cubicTo(0f, 0f, 0.35f, -0.05f, 0.5f, 0.2f)
    cubicTo(0.65f, -0.05f, 1f, 0f, 1f, 0.3f)
    cubicTo(1f, 0.57f, 0.65f, 0.8f, 0.5f, 0.95f); close()
}
