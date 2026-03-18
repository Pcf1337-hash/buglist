package com.buglist.domain.usecase

import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Result
import com.buglist.domain.repository.DebtRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddDebtUseCaseTest {

    private lateinit var debtRepository: DebtRepository
    private lateinit var useCase: AddDebtUseCase

    private fun makeDebt(
        personId: Long = 1L,
        amount: Double = 100.0,
        date: Long = System.currentTimeMillis()
    ) = DebtEntry(
        personId = personId,
        amount = amount,
        isOwedToMe = true,
        date = date
    )

    @Before
    fun setup() {
        debtRepository = mockk()
        useCase = AddDebtUseCase(debtRepository)
    }

    @Test
    fun `returns success with id for valid debt`() = runTest {
        coEvery { debtRepository.addDebtEntry(any()) } returns 42L

        val result = useCase(makeDebt())

        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data)
    }

    @Test
    fun `always saves with OPEN status`() = runTest {
        coEvery { debtRepository.addDebtEntry(any()) } returns 1L

        useCase(makeDebt())

        coVerify { debtRepository.addDebtEntry(match { it.status == DebtStatus.OPEN }) }
    }

    @Test
    fun `returns error when amount is zero`() = runTest {
        val result = useCase(makeDebt(amount = 0.0))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("0", ignoreCase = true))
    }

    @Test
    fun `returns error when amount is negative`() = runTest {
        val result = useCase(makeDebt(amount = -10.0))

        assertTrue(result is Result.Error)
    }

    @Test
    fun `returns error when amount exceeds maximum`() = runTest {
        val result = useCase(makeDebt(amount = 1_000_000.0))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("999", ignoreCase = true))
    }

    @Test
    fun `accepts maximum valid amount`() = runTest {
        coEvery { debtRepository.addDebtEntry(any()) } returns 1L

        val result = useCase(makeDebt(amount = 999_999.99))

        assertTrue(result is Result.Success)
    }

    @Test
    fun `returns error when personId is zero`() = runTest {
        val result = useCase(makeDebt(personId = 0))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("person", ignoreCase = true))
    }

    @Test
    fun `returns error when date is zero`() = runTest {
        val result = useCase(makeDebt(date = 0))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("date", ignoreCase = true))
    }

    @Test
    fun `returns error when repository throws`() = runTest {
        coEvery { debtRepository.addDebtEntry(any()) } throws RuntimeException("DB error")

        val result = useCase(makeDebt())

        assertTrue(result is Result.Error)
    }
}
