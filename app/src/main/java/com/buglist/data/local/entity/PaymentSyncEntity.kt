package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_sync",
    foreignKeys = [
        ForeignKey(
            entity = PaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["syncId"], unique = true)]
)
data class PaymentSyncEntity(
    @PrimaryKey val paymentId: Long,
    val syncId: String,
    val updatedAt: Long
)
