package com.buglist.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val grainBrush = rememberGrainBrush()
    MaterialTheme(
        colorScheme = BugListDarkColorScheme,
        typography = BugListTypography,
        shapes = BugListShapes
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .grainOverlay(grainBrush)
        ) {
            content()
        }
    }
}
