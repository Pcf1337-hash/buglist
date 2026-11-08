package com.buglist.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.util.KEY_AMBIENT_MOTION
import com.buglist.util.appDataStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp

/**
 * BugList v2.0.0 Material 3 Dark Color Scheme.
 *
 * Maps the street-style design palette to Material 3 color roles.
 * There is NO light theme – this app is always dark.
 */
private val BugListShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val BugListDarkColorScheme = darkColorScheme(
    primary            = BugListColors.Gold,
    onPrimary          = BugListColors.SurfaceDark,
    primaryContainer   = BugListColors.GoldDim,
    onPrimaryContainer = BugListColors.SurfaceDark,

    secondary            = BugListColors.TextPrimary,
    onSecondary          = BugListColors.SurfaceDark,
    secondaryContainer   = BugListColors.SurfaceElevated,
    onSecondaryContainer = BugListColors.TextPrimary,

    tertiary   = BugListColors.DebtGreen,
    onTertiary = BugListColors.SurfaceDark,

    error   = BugListColors.DebtRed,
    onError = BugListColors.SurfaceDark,

    background   = BugListColors.SurfaceDark,
    onBackground = BugListColors.TextPrimary,

    surface          = BugListColors.SurfaceCard,
    onSurface        = BugListColors.TextPrimary,
    surfaceVariant   = BugListColors.SurfaceElevated,
    onSurfaceVariant = BugListColors.TextSecondary,

    outline        = BugListColors.BorderSubtle,
    outlineVariant = BugListColors.TextMuted,

    surfaceContainerLowest = BugListColors.SurfaceDark,
    surfaceContainerLow = BugListColors.SurfaceCard,
    surfaceContainer = BugListColors.Surface,
    surfaceContainerHigh = BugListColors.SurfaceElevated,
    surfaceContainerHighest = BugListColors.SurfaceHigh,

    scrim            = BugListColors.SurfaceDark,
    inverseSurface   = BugListColors.TextPrimary,
    inverseOnSurface = BugListColors.SurfaceDark,
    inversePrimary   = BugListColors.GoldDim,
)

private fun DrawScope.drawAmbient(shift: AmbientShift) {
    val ox = shift.xDp.dp.toPx()
    val oy = shift.yDp.dp.toPx()
    drawRect(Color(0xFF070B14))
    drawRect(
        Brush.verticalGradient(
            listOf(
                Color(0xFF101A2C),
                Color(0xFF070B14),
                Color(0xFF0A1220)
            )
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x553A9CB8), Color(0x223A8CA8), Color.Transparent),
            center = Offset(size.width * 0.55f + ox, size.height * 0.22f + oy),
            radius = size.maxDimension * 0.85f
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x331C3E68), Color.Transparent),
            center = Offset(size.width * 0.12f - ox * 0.4f, size.height * 0.88f - oy * 0.4f),
            radius = size.width * 0.7f
        )
    )
}

/**
 * BugList application theme.
 *
 * Always dark. Forces [darkColorScheme] regardless of system settings.
 * Google Fonts (Oswald, Roboto Condensed, Bebas Neue) loaded via [BugListTypography].
 */
@Composable
fun BugListTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val shift = remember { AmbientShift() }
    val motionOn by produceState(initialValue = true, context) {
        context.appDataStore.data.collect { value = it[KEY_AMBIENT_MOTION] ?: true }
    }
    val holds by AmbientGates.holds.collectAsStateWithLifecycle()
    TrackAmbientTilt(shift = shift, enabled = motionOn, paused = holds > 0)
    MaterialTheme(
        colorScheme = BugListDarkColorScheme,
        typography = BugListTypography,
        shapes = BugListShapes
    ) {
        CompositionLocalProvider(LocalAmbientShift provides shift) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind { drawAmbient(shift) }
            ) {
                content()
            }
        }
    }
}
