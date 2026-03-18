package com.buglist.domain.repository

import com.buglist.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for [Tag] management and debt-entry tag associations.
 *
 * All read operations return [Flow] for reactive updates.
 * Write operations are suspend functions.
 *
 * The domain layer depends only on this interface — data layer details
 * (Room DAO, cross-ref entity) are hidden in the implementation.
 */
interface TagRepository {

    /**
     * Emits the full list of tags, ordered alphabetically by name.
     * Updates automatically when tags are inserted or deleted.
     */
    fun getAllTags(): Flow<List<Tag>>

    /**
     * Inserts a new tag. If a tag with the same name already exists the insert
     * is silently ignored (IGNORE conflict strategy).
     *
     * @return The row ID of the newly inserted tag, or -1 on conflict.
     */
    suspend fun insertTag(tag: Tag): Long

    /**
     * Permanently deletes a tag.
     * All cross-references to debt entries are removed via CASCADE on the DB level.
     *
     * @param tag Tag to delete (matched by [Tag.id]).
     */
    suspend fun deleteTag(tag: Tag)

    /**
     * Returns the tags currently attached to the given debt entry.
     *
     * @param debtEntryId FK reference to the debt entry.
     * @return List of [Tag] objects ordered alphabetically.
     */
    suspend fun getTagsForDebtEntry(debtEntryId: Long): List<Tag>

    /**
     * Replaces all tag associations for a debt entry with the given tag IDs.
     * Deletes the existing cross-refs first, then inserts the new ones atomically.
     *
     * @param debtEntryId FK reference to the debt entry.
     * @param tagIds      IDs of the tags to attach. Empty list removes all tags.
     */
    suspend fun setTagsForDebtEntry(debtEntryId: Long, tagIds: List<Long>)

    /**
     * Inserts the default tag set if the tags table is empty.
     * Safe to call multiple times — only inserts when the table is empty.
     */
    suspend fun insertDefaultTagsIfEmpty()
}
