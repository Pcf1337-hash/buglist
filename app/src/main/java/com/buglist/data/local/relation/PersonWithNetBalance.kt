package com.buglist.data.local.relation

import androidx.room.Embedded
import com.buglist.data.local.entity.PersonEntity

/**
 * Room projection combining a [PersonEntity] with its computed balance columns.
 *
 * Used by [PersonDao.getAllPersonsWithBalance] which executes the net-balance
 * aggregation query in a single JOIN pass — no N+1 queries.
 */
data class PersonWithNetBalance(
    @Embedded val person: PersonEntity,
    val netBalance: Double,
    val openCount: Int
)
