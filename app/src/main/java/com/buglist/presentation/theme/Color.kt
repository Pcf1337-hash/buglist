package com.buglist.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * BugList color tokens. Always dark.
 *
 * Deep navy black, with a quiet neon cyan as the accent. The name [Gold] is
 * kept so existing call sites stay the accent. Red and green stay the two
 * money colors and are not the accent: red means "schuldet mir", green means
 * "ich schulde" (L-081).
 *
 * Text and accent pairs were checked against WCAG 2.2 AA (4.5:1) on
 * [SurfaceDark] and [SurfaceCard].
 */
object BugListColors {
    // ── Backgrounds ──────────────────────────────────────────────────────────
    val Background  = Color(0xFF070B14)
    val SurfaceDark = Color(0xFF070B14)
    val SurfaceCard = Color(0xFF0E1626)
    val SurfaceElevated = Color(0xFF17243A)
    val SurfaceOverlay = Color(0xFF1C2C46)

    val Surface     = Color(0xFF121C30)
    val SurfaceHigh = Color(0xFF1C2C46)

    // ── Accent (token name Gold is historical) ───────────────────────────────
    val Gold        = Color(0xFF7AD7F0)
    val GoldDim     = Color(0xFF6AA0B4)
    val GoldGlow    = Color(0x337AD7F0)
    val BorderGold  = Color(0x807AD7F0)

    // ── Text ──────────────────────────────────────────────────────────────────
    val TextPrimary   = Color(0xFFF4F8FB)
    val TextSecondary = Color(0xFFC5D4E0)
    val TextMuted     = Color(0xFF9BB0C2)

    val Platinum    = Color(0xFFE7EEF4)
    val Muted       = Color(0xFF9BB0C2)

    // ── Debt colors. Kept apart from the cyan accent. ────────────────────────
    val DebtGreen    = Color(0xFF3DDC97)
    val DebtGreenDim = Color(0xFF1FA87A)
    val DebtRed      = Color(0xFFFF6B86)
    val DebtRedDim   = Color(0xFFD63A52)

    // ── Status ────────────────────────────────────────────────────────────────
    val StatusOpen      = Color(0xFF7AD7F0)
    val StatusPartial   = Color(0xFFF0B429)
    val StatusPaid      = Color(0xFF3DDC97)
    val StatusCancelled = Color(0xFF9BB0C2)
    val Orange          = Color(0xFFF0B429)

    // ── Borders / Separators ──────────────────────────────────────────────────
    val BorderSubtle = Color(0xFF24344C)
    val Divider      = Color(0xFF24344C)

    // ── Misc ──────────────────────────────────────────────────────────────────
    val White       = Color(0xFFFFFFFF)
    val Transparent = Color(0x00000000)
}
