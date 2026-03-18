package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `payments` table.
 *
 * Each payment represents a partial (or full) settlement of a [DebtEntryEntity].
 * Foreign key to [DebtEntryEntity] with CASCADE DELETE — deleting a debt entry
 * automatically removes all associated payment records.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["debtEntryId"]),
        Index(value = ["date"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtEntryId: Long,        // FK → DebtEntryEntity, CASCADE DELETE
    val amount: Double,           // Partial payment amount, always positive
    val note: String? = null,
    val date: Long = System.currentTimeMillis()
)
