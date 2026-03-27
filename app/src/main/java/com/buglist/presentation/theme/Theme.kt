package com.buglist.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * BugList v2.0.0 Material 3 Dark Color Scheme.
 *
 * Maps the street-style design palette to Material 3 color roles.
 * There is NO light theme – this app is always dark.
 */
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
    MaterialTheme(
        colorScheme = BugListDarkColorScheme,
        typography = BugListTypography,
        content = content
    )
}
