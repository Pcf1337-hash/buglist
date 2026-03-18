package com.buglist.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * BugList Design System color palette.
 * Always dark – no light mode support.
 * Gangster-Rap / Streetstyle aesthetic.
 */
object BugListColors {
    val Background  = Color(0xFF0D0D0D)  // Deep black background
    val Surface     = Color(0xFF1A1A1A)  // Cards and surfaces
    val SurfaceHigh = Color(0xFF242424)  // Elevated cards
    val Gold        = Color(0xFFFFD700)  // Primary accent – FAB, CTAs, headings
    val GoldDim     = Color(0xFFB8960C)  // Disabled / secondary gold
    val Platinum    = Color(0xFFE5E4E2)  // Primary text
    val Muted       = Color(0xFF666666)  // Secondary text / labels
    val DebtRed     = Color(0xFFFF3B3B)  // Negative amounts / "I owe"
    val DebtGreen   = Color(0xFF00E676)  // Positive amounts / "owes me"
    val Divider     = Color(0xFF2A2A2A)  // Separator lines
    val Orange      = Color(0xFFFF8C00)  // PARTIAL status border
    val White       = Color(0xFFFFFFFF)
    val Transparent = Color(0x00000000)
}
