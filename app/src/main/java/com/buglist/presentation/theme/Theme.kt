package com.buglist.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * BugList Material 3 Dark Color Scheme.
 *
 * Maps the street-style design palette to Material 3 color roles.
 * There is NO light theme – this app is always dark.
 */
private val BugListDarkColorScheme = darkColorScheme(
    primary          = BugListColors.Gold,
    onPrimary        = BugListColors.Background,
    primaryContainer = BugListColors.GoldDim,
    onPrimaryContainer = BugListColors.Background,

    secondary        = BugListColors.Platinum,
    onSecondary      = BugListColors.Background,
    secondaryContainer = BugListColors.SurfaceHigh,
    onSecondaryContainer = BugListColors.Platinum,

    tertiary         = BugListColors.DebtGreen,
    onTertiary       = BugListColors.Background,

    error            = BugListColors.DebtRed,
    onError          = BugListColors.Background,

    background       = BugListColors.Background,
    onBackground     = BugListColors.Platinum,

    surface          = BugListColors.Surface,
    onSurface        = BugListColors.Platinum,
    surfaceVariant   = BugListColors.SurfaceHigh,
    onSurfaceVariant = BugListColors.Muted,

    outline          = BugListColors.Divider,
    outlineVariant   = BugListColors.Muted,

    scrim            = BugListColors.Background,
    inverseSurface   = BugListColors.Platinum,
    inverseOnSurface = BugListColors.Background,
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
