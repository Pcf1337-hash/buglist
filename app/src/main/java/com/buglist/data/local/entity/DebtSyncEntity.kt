package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_sync",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["syncId"], unique = true)]
)
data class DebtSyncEntity(
    @PrimaryKey val debtEntryId: Long,
    val syncId: String,
    val updatedAt: Long
)
