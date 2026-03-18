package com.buglist.domain.usecase

import com.buglist.domain.model.Person
import com.buglist.domain.model.Result
import com.buglist.domain.repository.PersonRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddPersonUseCaseTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var useCase: AddPersonUseCase

    @Before
    fun setup() {
        personRepository = mockk()
        useCase = AddPersonUseCase(personRepository)
    }

    @Test
    fun `returns success with id when valid person is added`() = runTest {
        val person = Person(name = "Alice")
        coEvery { personRepository.addPerson(any()) } returns 1L

        val result = useCase(person)

        assertTrue(result is Result.Success)
        assertEquals(1L, (result as Result.Success).data)
        coVerify { personRepository.addPerson(match { it.name == "Alice" }) }
    }

    @Test
    fun `trims name before saving`() = runTest {
        val person = Person(name = "  Alice  ")
        coEvery { personRepository.addPerson(any()) } returns 2L

        val result = useCase(person)

        assertTrue(result is Result.Success)
        coVerify { personRepository.addPerson(match { it.name == "Alice" }) }
    }

    @Test
    fun `returns error when name is blank`() = runTest {
        val person = Person(name = "   ")

        val result = useCase(person)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("empty", ignoreCase = true))
    }

    @Test
    fun `returns error when name is empty`() = runTest {
        val person = Person(name = "")

        val result = useCase(person)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `returns error when name exceeds 100 characters`() = runTest {
        val person = Person(name = "A".repeat(101))

        val result = useCase(person)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("100", ignoreCase = true))
    }

    @Test
    fun `accepts name exactly 100 characters`() = runTest {
        val person = Person(name = "A".repeat(100))
        coEvery { personRepository.addPerson(any()) } returns 3L

        val result = useCase(person)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `returns error when repository throws`() = runTest {
        val person = Person(name = "Bob")
        coEvery { personRepository.addPerson(any()) } throws RuntimeException("DB error")

        val result = useCase(person)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).cause is RuntimeException)
    }
}
