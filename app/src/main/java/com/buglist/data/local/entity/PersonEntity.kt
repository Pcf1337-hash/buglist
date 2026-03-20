package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `persons` table.
 *
 * Mirrors [com.buglist.domain.model.Person] but is a data-layer concern.
 * The domain model is never used as a Room entity (Clean Architecture boundary).
 */
@Entity(
    tableName = "persons",
    indices = [
        Index(value = ["name"]),
        Index(value = ["createdAt"])
    ]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val avatarColor: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Manual drag-and-drop sort position. Stored as `sort_index` column (added in schema v3).
     * Default [Int.MAX_VALUE] = unsorted (falls back to name order for ties in the SQL query).
     */
    val sortIndex: Int = Int.MAX_VALUE,
    /**
     * Absolute path to a custom avatar image in the app's internal files dir.
     * Null = no custom photo, show initials avatar. Added in schema v4.
     */
    val avatarImagePath: String? = null
)
