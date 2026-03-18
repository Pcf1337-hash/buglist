package com.buglist.domain.model

/**
 * Monthly aggregated statistics for the statistics chart.
 *
 * @param yearMonth      String in "YYYY-MM" format (e.g., "2026-03").
 * @param totalOwedToMe  Sum of original amounts where [DebtEntry.isOwedToMe] == true
 *                       and status != CANCELLED.
 * @param totalIOwe      Sum of original amounts where [DebtEntry.isOwedToMe] == false
 *                       and status != CANCELLED.
 */
data class MonthlyStats(
    val yearMonth: String,
    val totalOwedToMe: Double,
    val totalIOwe: Double
)
