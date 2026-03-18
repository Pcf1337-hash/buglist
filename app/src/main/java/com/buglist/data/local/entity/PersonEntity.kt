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
    val createdAt: Long = System.currentTimeMillis()
)
