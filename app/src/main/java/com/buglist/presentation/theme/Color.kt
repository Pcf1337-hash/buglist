package com.buglist.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * BugList v2.0.0 Design System color palette.
 * Always dark – no light mode support.
 * Gangster-Rap / Streetstyle aesthetic.
 */
object BugListColors {
    // ── Backgrounds ──────────────────────────────────────────────────────────
    val Background  = Color(0xFF0D0D0D)  // Deep black background (legacy compat)
    val SurfaceDark = Color(0xFF0A0A0A)  // True deep black – BiometricLockScreen
    val SurfaceCard = Color(0xFF141414)  // Card backgrounds
    val SurfaceElevated = Color(0xFF1C1C1C) // Elevated surfaces, numpad keys
    val SurfaceOverlay = Color(0xFF242424)  // Dialogs, sheets

    // Legacy aliases kept for backward compat with existing code
    val Surface     = Color(0xFF1A1A1A)  // Cards and surfaces
    val SurfaceHigh = Color(0xFF242424)  // Elevated cards

    // ── Gold / Primary ────────────────────────────────────────────────────────
    val Gold        = Color(0xFFFFD700)  // Primary accent – FAB, CTAs, headings
    val GoldDim     = Color(0xFFB8960C)  // Disabled / secondary gold
    val GoldGlow    = Color(0x33FFD700)  // Glow effect (20% alpha)
    val BorderGold  = Color(0x66FFD700)  // Gold border (40% alpha)

    // ── Text ──────────────────────────────────────────────────────────────────
    val TextPrimary   = Color(0xFFFFFFFF)  // Primary text
    val TextSecondary = Color(0xFFAAAAAA)  // Secondary text
    val TextMuted     = Color(0xFF666666)  // Muted / placeholder text

    // Legacy aliases
    val Platinum    = Color(0xFFE5E4E2)  // Primary text (legacy)
    val Muted       = Color(0xFF666666)  // Secondary text / labels (legacy)

    // ── Debt Colors ───────────────────────────────────────────────────────────
    val DebtGreen    = Color(0xFF00E676)  // Positive amounts / "owes me"
    val DebtGreenDim = Color(0xFF00C853)  // Dim green
    val DebtRed      = Color(0xFFFF1744)  // Negative amounts / "I owe" (updated for v2)
    val DebtRedDim   = Color(0xFFD50000)  // Dim red

    // ── Status Colors ─────────────────────────────────────────────────────────
    val StatusOpen      = Color(0xFFFFD700)  // OPEN status
    val StatusPartial   = Color(0xFFFF9800)  // PARTIAL status
    val StatusPaid      = Color(0xFF00E676)  // PAID status
    val StatusCancelled = Color(0xFF666666)  // CANCELLED status
    val Orange          = Color(0xFFFF9800)  // Orange (legacy compat)

    // ── Borders / Separators ──────────────────────────────────────────────────
    val BorderSubtle = Color(0xFF2A2A2A)  // Subtle border
    val Divider      = Color(0xFF2A2A2A)  // Separator lines (legacy compat)

    // ── Misc ──────────────────────────────────────────────────────────────────
    val White       = Color(0xFFFFFFFF)
    val Transparent = Color(0x00000000)
}
