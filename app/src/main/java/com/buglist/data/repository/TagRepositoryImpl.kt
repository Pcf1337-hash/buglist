package com.buglist.data.repository

import com.buglist.data.local.dao.TagDao
import com.buglist.data.local.entity.DebtEntryTagCrossRef
import com.buglist.data.local.entity.TagEntity
import com.buglist.data.local.entity.toDomain
import com.buglist.data.local.entity.toEntity
import com.buglist.domain.model.Tag
import com.buglist.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [TagRepository].
 *
 * Default tags are pre-seeded once at first launch via [insertDefaultTagsIfEmpty].
 * The default set is intended as a quick-start for common use cases; users can
 * add or remove tags freely in the Settings screen.
 */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    companion object {
        /** Default tags inserted when the tags table is empty (first launch). */
        private val DEFAULT_TAGS = listOf("Hase", "Cal", "E", "K", "P", "\u20AC")
    }

    /** Emits all tags ordered alphabetically. Reactive — updates on insert/delete. */
    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { list -> list.map { it.toDomain() } }

    /**
     * Inserts a tag. Returns the row ID or -1 on conflict.
     *
     * @param tag Tag to insert (id = 0 for auto-generation).
     */
    override suspend fun insertTag(tag: Tag): Long =
        tagDao.insertTag(tag.toEntity())

    /**
     * Permanently deletes a tag and all its debt-entry associations (via FK CASCADE).
     *
     * @param tag Tag to delete.
     */
    override suspend fun deleteTag(tag: Tag) =
        tagDao.deleteTag(tag.toEntity())

    /**
     * Returns the tags attached to the given debt entry, ordered alphabetically.
     *
     * @param debtEntryId FK reference to the debt entry.
     */
    override suspend fun getTagsForDebtEntry(debtEntryId: Long): List<Tag> =
        tagDao.getTagsForDebtEntry(debtEntryId).map { it.toDomain() }

    /**
     * Atomically replaces all tag associations for a debt entry.
     *
     * Clears existing cross-refs first, then inserts the new set.
     * An empty [tagIds] list removes all tags from the entry.
     *
     * @param debtEntryId FK reference to the debt entry.
     * @param tagIds      IDs of the tags to attach.
     */
    override suspend fun setTagsForDebtEntry(debtEntryId: Long, tagIds: List<Long>) {
        tagDao.clearTagsForDebtEntry(debtEntryId)
        if (tagIds.isNotEmpty()) {
            tagDao.insertCrossRefs(tagIds.map { DebtEntryTagCrossRef(debtEntryId, it) })
        }
    }

    /**
     * Seeds the default tags if the tags table is empty.
     * Idempotent — no-op when tags already exist.
     */
    override suspend fun insertDefaultTagsIfEmpty() {
        if (tagDao.getTagCount() == 0) {
            DEFAULT_TAGS.forEach { name ->
                tagDao.insertTag(TagEntity(name = name))
            }
        }
    }
}
