package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the `dividers` table (schema v5).
 *
 * Stores decorative separator rows that appear alongside person entries in the
 * Dashboard crew list. Both tables share the same `sortIndex` space so that
 * drag-to-reorder works across persons and dividers uniformly.
 *
 * Added via [com.buglist.data.local.AppDatabase.MIGRATION_4_5].
 */
@Entity(tableName = "dividers")
data class DividerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Text shown centred on the divider line. May be empty string for line-only separators. */
    val label: String,

    /** ARGB color int for both the line and the label text. Default: Gold (#FFD700). */
    val color: Int = 0xFFFFD700.toInt(),

    /** Line style stored as string name of [com.buglist.domain.model.DividerLineStyle]. */
    val lineStyle: String = "SOLID",

    /**
     * Manual sort position. Shares the value space with [PersonEntity.sortIndex].
     * Default [Int.MAX_VALUE] = unsorted (appears at bottom before first drag-to-reorder).
     */
    val sortIndex: Int = Int.MAX_VALUE
)
