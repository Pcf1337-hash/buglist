package com.buglist.domain.model

/**
 * Visual line style for a [Divider] separator row in the crew list.
 *
 * - [SOLID]  – single solid line (1 dp stroke)
 * - [DASHED] – dashed line (dash 10 px, gap 6 px)
 * - [THICK]  – solid line with 3 dp stroke
 */
enum class DividerLineStyle { SOLID, DASHED, THICK }

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
