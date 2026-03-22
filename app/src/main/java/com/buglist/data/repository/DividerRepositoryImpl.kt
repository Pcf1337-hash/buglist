package com.buglist.data.repository

import com.buglist.data.local.dao.DividerDao
import com.buglist.data.local.entity.DividerEntity
import com.buglist.domain.model.Divider
import com.buglist.domain.model.DividerLineStyle
import com.buglist.domain.repository.DividerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DividerRepository] backed by Room + SQLCipher.
 *
 * All mapping between [Divider] domain models and [DividerEntity] Room entities
 * is contained here. The domain layer never sees Room annotations.
 */
@Singleton
class DividerRepositoryImpl @Inject constructor(
    private val dividerDao: DividerDao
) : DividerRepository {

    override fun getAllDividers(): Flow<List<Divider>> =
        dividerDao.getAllDividers().map { list -> list.map { it.toDomain() } }

    override suspend fun addDivider(divider: Divider): Long =
        dividerDao.insert(divider.toEntity())

    override suspend fun updateDivider(divider: Divider) =
        dividerDao.update(divider.toEntity())

    override suspend fun deleteDivider(id: Long) =
        dividerDao.deleteById(id)

    override suspend fun updateSortIndex(id: Long, sortIndex: Int) =
        dividerDao.updateSortIndex(id, sortIndex)
}

// --- Mapping extensions ---

private fun DividerEntity.toDomain(): Divider = Divider(
    id = id,
    label = label,
    color = color,
    lineStyle = when (lineStyle) {
        "DASHED" -> DividerLineStyle.DASHED
        "THICK"  -> DividerLineStyle.THICK
        else     -> DividerLineStyle.SOLID
    },
    sortIndex = sortIndex
)

private fun Divider.toEntity(): DividerEntity = DividerEntity(
    id = id,
    label = label,
    color = color,
    lineStyle = lineStyle.name,
    sortIndex = sortIndex
)
