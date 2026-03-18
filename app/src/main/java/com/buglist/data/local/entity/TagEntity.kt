package com.buglist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.buglist.domain.model.Tag

/**
 * Room entity for the `tags` table.
 *
 * Tags are user-defined labels (max. 20 characters) that can be attached
 * to debt entries via the [DebtEntryTagCrossRef] join table.
 *
 * @param id        Auto-generated primary key.
 * @param name      Display name of the tag.
 * @param createdAt Insertion timestamp in Unix ms.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Maps a [TagEntity] to its domain counterpart [Tag]. */
fun TagEntity.toDomain() = Tag(id = id, name = name, createdAt = createdAt)

/** Maps a domain [Tag] to the [TagEntity] Room entity. */
fun Tag.toEntity() = TagEntity(id = id, name = name, createdAt = createdAt)
