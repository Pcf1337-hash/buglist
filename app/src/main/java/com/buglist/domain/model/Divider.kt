package com.buglist.domain.model

/**
 * Visual rendering style for a [Divider] separator row in the crew list.
 *
 * - [SOLID]           – `──── LABEL ────`            single solid line
 * - [DASHED]          – `- - - LABEL - - -`          dashed line
 * - [THICK]           – `━━━━ LABEL ━━━━`            thick (3 dp) solid line
 * - [DIAMOND_STAR]    – `──── ✦ LABEL ✦ ────`       star-diamond flanked label
 * - [BRACKET]         – `──── [ LABEL ] ────`        bracket flanked label
 * - [ARROW]           – `<<< LABEL >>>`              no lines, arrow text only
 * - [DIAMOND_FLANKED] – `❖──── LABEL ────❖`         outer diamond flanking
 */
enum class DividerLineStyle {
    SOLID,
    DASHED,
    THICK,
    DIAMOND_STAR,
    BRACKET,
    ARROW,
    DIAMOND_FLANKED
}

/**
 * Domain model for a decorative separator item in the crew list.
 *
 * Dividers appear between or around [Person] entries and carry an optional label,
 * a color, and a line style. They are stored in their own `dividers` table so
 * that they participate in the same manual drag-to-reorder mechanism as persons.
 *
 * @param id        Auto-generated primary key (0 = not yet persisted).
 * @param label     Optional text shown centred on the divider line. May be blank.
 * @param color     ARGB color int for both the line and the label text.
 * @param lineStyle Visual style of the separator line.
 * @param sortIndex Manual sort position (shared space with [Person.sortIndex]).
 *                  Lower value → higher in the list. Default [Int.MAX_VALUE] = bottom.
 */
data class Divider(
    val id: Long = 0,
    val label: String,
    val color: Int = 0xFFFFD700.toInt(),
    val lineStyle: DividerLineStyle = DividerLineStyle.SOLID,
    val sortIndex: Int = Int.MAX_VALUE
)
