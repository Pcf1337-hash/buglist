package com.buglist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.buglist.data.local.entity.DividerEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [DividerEntity].
 *
 * Reads return [Flow] for reactive updates.
 * Writes are `suspend fun` — must be called from a coroutine context.
 */
@Dao
interface DividerDao {

    /** Inserts a new divider. Returns the auto-generated row ID. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(divider: DividerEntity): Long

    /** Updates an existing divider (label, color, lineStyle, sortIndex). */
    @Update
    suspend fun update(divider: DividerEntity)

    /** Deletes a divider by ID. */
    @Query("DELETE FROM dividers WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Returns all dividers ordered by [DividerEntity.sortIndex] ascending. */
    @Query("SELECT * FROM dividers ORDER BY sortIndex ASC")
    fun getAllDividers(): Flow<List<DividerEntity>>

    /**
     * Updates the manual sort position of a single divider.
     * Used by [com.buglist.data.repository.DividerRepositoryImpl.updateSortIndex]
     * during drag-to-reorder save.
     */
    @Query("UPDATE dividers SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun updateSortIndex(id: Long, sortIndex: Int)
}
