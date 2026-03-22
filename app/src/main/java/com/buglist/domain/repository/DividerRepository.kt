package com.buglist.domain.repository

import com.buglist.domain.model.Divider
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for [Divider] operations.
 *
 * Lives in the domain layer — no Room or data-layer imports here.
 * Implementation: [com.buglist.data.repository.DividerRepositoryImpl].
 */
interface DividerRepository {

    /**
     * Live stream of all dividers ordered by [Divider.sortIndex] ascending.
     */
    fun getAllDividers(): Flow<List<Divider>>

    /**
     * Inserts a new divider. Returns the auto-generated row ID.
     */
    suspend fun addDivider(divider: Divider): Long

    /**
     * Updates an existing divider record (label, color, lineStyle).
     */
    suspend fun updateDivider(divider: Divider)

    /**
     * Permanently removes the divider with the given [id].
     */
    suspend fun deleteDivider(id: Long)

    /**
     * Updates the [Divider.sortIndex] of a single divider.
     * Called during drag-to-reorder save to persist combined list positions.
     */
    suspend fun updateSortIndex(id: Long, sortIndex: Int)
}
