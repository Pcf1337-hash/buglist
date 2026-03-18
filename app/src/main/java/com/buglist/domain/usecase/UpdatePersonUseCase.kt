package com.buglist.domain.usecase

import com.buglist.domain.model.Person
import com.buglist.domain.model.Result
import com.buglist.domain.repository.PersonRepository
import javax.inject.Inject

/**
 * Use case for updating an existing person's details.
 *
 * Applies the same name validation as [AddPersonUseCase].
 * The person must have a valid id (> 0).
 */
class UpdatePersonUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    suspend operator fun invoke(person: Person): Result<Unit> {
        if (person.id <= 0) {
            return Result.Error("Cannot update a person without a valid ID")
        }
        val name = person.name.trim()
        if (name.isBlank()) {
            return Result.Error("Name must not be empty")
        }
        if (name.length > 100) {
            return Result.Error("Name must not exceed 100 characters")
        }
        return try {
            personRepository.updatePerson(person.copy(name = name))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update person: ${e.message}", e)
        }
    }
}
