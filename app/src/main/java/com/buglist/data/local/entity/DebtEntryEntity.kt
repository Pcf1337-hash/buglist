package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `debt_entries` table.
 *
 * Foreign key to [PersonEntity] with CASCADE DELETE — when a person is deleted,
 * all their debt entries are automatically removed.
 *
 * Status values: "OPEN", "PARTIAL", "PAID", "CANCELLED"
 * (stored as String to remain readable in SQLite PRAGMA output during dev checks).
 */
@Entity(
    tableName = "debt_entries",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["personId"]),
        Index(value = ["date"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class DebtEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val amount: Double,           // Original amount, always positive
    val currency: String = "EUR",
    val isOwedToMe: Boolean,      // true = person owes me, false = I owe them
    val description: String? = null,
    val date: Long,
    val dueDate: Long? = null,
    val status: String = "OPEN",  // OPEN | PARTIAL | PAID | CANCELLED
    val createdAt: Long = System.currentTimeMillis()
)
