package com.buglist.domain.usecase

import com.buglist.domain.model.Person
import com.buglist.domain.model.Result
import com.buglist.domain.repository.PersonRepository
import javax.inject.Inject

/**
 * Use case for deleting a person from the ledger.
 *
 * Deleting a person will CASCADE DELETE all their debt entries and associated payments
 * (enforced at the DB level via foreign key constraints).
 */
class DeletePersonUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    suspend operator fun invoke(person: Person): Result<Unit> {
        if (person.id <= 0) {
            return Result.Error("Cannot delete a person without a valid ID")
        }
        return try {
            personRepository.deletePerson(person)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete person: ${e.message}", e)
        }
    }
}
