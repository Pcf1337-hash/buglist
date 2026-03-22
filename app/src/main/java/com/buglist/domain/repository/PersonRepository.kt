package com.buglist.domain.repository

import com.buglist.domain.model.Person
import com.buglist.domain.model.PersonWithBalance
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for [Person] operations.
 *
 * This interface lives in the domain layer — it knows nothing about Room, DAOs,
 * or any data-layer implementation details.
 *
 * The implementation is in [com.buglist.data.repository.PersonRepositoryImpl].
 */
interface PersonRepository {

    /**
     * Returns a live stream of all persons with their computed net balance,
     * ordered by name ascending.
     *
     * Net balance is based on **remaining** amounts (original minus payments) for
     * non-terminal (non-PAID, non-CANCELLED) debts only.
     */
    fun getAllPersonsWithBalance(): Flow<List<PersonWithBalance>>

    /**
     * Returns a live stream of a single person by [id], or null if not found.
     */
    fun getPersonById(id: Long): Flow<Person?>

    /**
     * Returns a live stream of the total person count.
     */
    fun getPersonCount(): Flow<Int>

    /**
     * Inserts a new person. Returns the auto-generated row ID.
     *
     * @throws IllegalArgumentException if [person.name] is blank or > 100 chars.
     */
    suspend fun addPerson(person: Person): Long

    /**
     * Updates an existing person record.
     */
    suspend fun updatePerson(person: Person)

    /**
     * Deletes a person and all their debt entries (CASCADE DELETE).
     */
    suspend fun deletePerson(person: Person)

    /**
     * Deletes a person by ID (CASCADE DELETE).
     */
    suspend fun deletePersonById(personId: Long)

    /**
     * Persists the manual drag-to-reorder result for the crew list.
     *
     * Iterates through [orderedIds] in order and writes each person's new
     * [Person.sortIndex] (= list position 0, 1, 2, …) to the database.
     * After this call the DB query ORDER BY `sortIndex ASC, name ASC` will
     * return persons in the user's chosen order.
     *
     * @param orderedIds Person IDs in the new desired order (top → bottom).
     */
    suspend fun updatePersonSortIndices(orderedIds: List<Long>)

    /**
     * Updates the [Person.sortIndex] of a single person.
     *
     * Used when persisting the combined person+divider drag-to-reorder result,
     * where each item receives a sortIndex equal to its position in the merged list.
     *
     * @param id        The person's primary key.
     * @param sortIndex The new sort position.
     */
    suspend fun updatePersonSortIndex(id: Long, sortIndex: Int)
}
