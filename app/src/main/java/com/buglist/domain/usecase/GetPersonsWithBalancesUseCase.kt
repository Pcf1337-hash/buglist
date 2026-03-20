package com.buglist.domain.usecase

import com.buglist.domain.model.PersonWithBalance
import com.buglist.domain.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Sort order for the persons list.
 */
enum class PersonSortOrder {
    /**
     * Manual drag-and-drop order defined by the user (sortIndex column).
     * The DB query already returns persons in sortIndex ASC, name ASC order,
     * so this sort order simply preserves the DB result as-is.
     */
    MANUAL,

    /** Alphabetical by name. */
    NAME,

    /** By net balance descending (highest debt first). */
    BALANCE_DESC,

    /** By most recently created first. */
    CREATED_AT_DESC
}

/**
 * Use case that returns a live stream of all persons with their computed balances.
 *
 * The underlying query already computes net balances in SQL.
 * This use case adds optional client-side sorting.
 *
 * @param sortOrder Client-side sort. Default is [PersonSortOrder.NAME].
 */
class GetPersonsWithBalancesUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    operator fun invoke(sortOrder: PersonSortOrder = PersonSortOrder.MANUAL): Flow<List<PersonWithBalance>> =
        personRepository.getAllPersonsWithBalance().map { persons ->
            when (sortOrder) {
                PersonSortOrder.MANUAL -> persons          // DB already returns sortIndex ASC, name ASC
                PersonSortOrder.NAME -> persons.sortedBy { it.person.name.lowercase() }
                PersonSortOrder.BALANCE_DESC -> persons.sortedByDescending { it.netBalance }
                PersonSortOrder.CREATED_AT_DESC -> persons.sortedByDescending { it.person.createdAt }
            }
        }
}
