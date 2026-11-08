package com.buglist.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

private fun DrawScope.drawAmbient() {
    drawRect(
        Brush.verticalGradient(
            listOf(
                Color(0xFF0C1828),
                Color(0xFF070B14),
                Color(0xFF080E18)
            )
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x333A8CA8), Color.Transparent),
            center = Offset(size.width * 0.72f, size.height * 0.06f),
            radius = size.width * 0.95f
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x221C3E68), Color.Transparent),
            center = Offset(size.width * 0.08f, size.height * 0.92f),
            radius = size.width * 0.8f
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
    MaterialTheme(
        colorScheme = BugListDarkColorScheme,
        typography = BugListTypography,
        shapes = BugListShapes
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawAmbient() }
        ) {
            content()
        }
    }
}
