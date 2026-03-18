package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Many-to-many join entity connecting [DebtEntryEntity] and [TagEntity].
 *
 * Composite primary key `(debtEntryId, tagId)` prevents duplicate associations.
 * Foreign keys with CASCADE DELETE ensure that cross-refs are automatically
 * removed when either the debt entry or the tag is deleted.
 *
 * Indices on both FK columns keep JOIN queries fast even with large datasets.
 */
@Entity(
    tableName = "debt_entry_tags",
    primaryKeys = ["debtEntryId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = DebtEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtEntryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("tagId"),
        Index("debtEntryId")
    ]
)
data class DebtEntryTagCrossRef(
    val debtEntryId: Long,
    val tagId: Long
)
