package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "pairings",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PairingEntity(
    @PrimaryKey val personId: Long,
    val role: String,
    val roomId: String,
    val keyMaterial: String,
    val deviceId: String,
    val lastPushedHash: String,
    val linkedAt: Long
)
