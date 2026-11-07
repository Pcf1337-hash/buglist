package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Hidden removal marker so the other phone drops a debt or payment.
 * This is not shown anywhere in the UI.
 */
@Entity(
    tableName = "sync_tombstones",
    indices = [
        Index(value = ["personId"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class SyncTombstoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val syncId: String,
    val kind: String,
    val parentSyncId: String?,
    val updatedAt: Long
)
