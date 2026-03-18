package com.buglist.domain.usecase

import com.buglist.domain.repository.DebtRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateTotalBalanceUseCaseTest {

    private lateinit var debtRepository: DebtRepository
    private lateinit var useCase: CalculateTotalBalanceUseCase

    @Before
    fun setup() {
        debtRepository = mockk()
        useCase = CalculateTotalBalanceUseCase(debtRepository)
    }

    @Test
    fun `returns positive balance when money is owed to user`() = runTest {
        every { debtRepository.getTotalNetBalance() } returns flowOf(500.0)

        val balance = useCase().first()

        assertEquals(500.0, balance, 0.001)
    }

    @Test
    fun `returns negative balance when user owes money`() = runTest {
        every { debtRepository.getTotalNetBalance() } returns flowOf(-200.0)

        val balance = useCase().first()

        assertEquals(-200.0, balance, 0.001)
    }

    @Test
    fun `returns zero when all debts are settled`() = runTest {
        every { debtRepository.getTotalNetBalance() } returns flowOf(0.0)

        val balance = useCase().first()

        assertEquals(0.0, balance, 0.001)
    }
}
