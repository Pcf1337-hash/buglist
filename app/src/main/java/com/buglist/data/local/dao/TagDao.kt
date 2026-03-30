package com.buglist.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buglist.data.local.entity.DebtEntryTagCrossRef
import com.buglist.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [TagEntity] and [DebtEntryTagCrossRef] operations.
 *
 * Follow Clean Architecture rules from CLAUDE.md:
 * - Queries return [Flow] for reactive UI updates.
 * - Write operations are `suspend fun`.
 */
@Dao
interface TagDao {

    /**
     * Emits all tags ordered alphabetically by name.
     * Updates reactively on insert/delete.
     */
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    /**
     * Inserts a tag. Silently ignores duplicates (IGNORE strategy).
     *
     * @return The row ID of the inserted tag, or -1 on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    /**
     * Permanently deletes a tag.
     * All [DebtEntryTagCrossRef] rows referencing this tag are removed via CASCADE.
     */
    @Delete
    suspend fun deleteTag(tag: TagEntity)

    /**
     * Returns all tags attached to the given debt entry, ordered alphabetically.
     *
     * @param debtEntryId FK reference to the `debt_entries` table.
     */
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN debt_entry_tags det ON t.id = det.tagId
        WHERE det.debtEntryId = :debtEntryId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsForDebtEntry(debtEntryId: Long): List<TagEntity>

    /**
     * Removes all tag associations for a given debt entry.
     * Called before re-inserting updated associations in [insertCrossRefs].
     */
    @Query("DELETE FROM debt_entry_tags WHERE debtEntryId = :debtEntryId")
    suspend fun clearTagsForDebtEntry(debtEntryId: Long)

    /**
     * Inserts or replaces a batch of [DebtEntryTagCrossRef] rows.
     * Uses REPLACE to handle edge cases where the same pair is inserted twice.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<DebtEntryTagCrossRef>)

    /**
     * Returns the total number of tags in the table.
     * Used by [insertDefaultTagsIfEmpty] to decide whether seeding is needed.
     */
    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int

    /**
     * Returns a tag by its exact name (case-sensitive), or null if it does not exist.
     * Used by [ImportDebtListUseCase] to resolve tag names to IDs after insert.
     */
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?
}
