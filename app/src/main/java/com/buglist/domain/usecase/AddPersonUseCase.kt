package com.buglist.domain.usecase

import com.buglist.domain.model.Person
import com.buglist.domain.model.Result
import com.buglist.domain.repository.PersonRepository
import javax.inject.Inject

/**
 * Use case for adding a new person to the ledger.
 *
 * Validation rules:
 * - Name must not be blank.
 * - Name must not exceed 100 characters.
 *
 * @return [Result.Success] with the new person's ID, or [Result.Error] on validation failure.
 */
class AddPersonUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    suspend operator fun invoke(person: Person): Result<Long> {
        val name = person.name.trim()
        if (name.isBlank()) {
            return Result.Error("Name must not be empty")
        }
        if (name.length > 100) {
            return Result.Error("Name must not exceed 100 characters")
        }
        return try {
            val id = personRepository.addPerson(person.copy(name = name))
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error("Failed to add person: ${e.message}", e)
        }
    }
}
