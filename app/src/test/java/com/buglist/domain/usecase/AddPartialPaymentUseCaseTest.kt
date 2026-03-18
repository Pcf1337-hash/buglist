package com.buglist.domain.usecase

import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Payment
import com.buglist.domain.model.Result
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.PaymentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddPartialPaymentUseCaseTest {

    private lateinit var paymentRepository: PaymentRepository
    private lateinit var debtRepository: DebtRepository
    private lateinit var useCase: AddPartialPaymentUseCase

    private fun makeOpenDebt(amount: Double = 100.0, paid: Double = 0.0): DebtEntryWithPayments {
        val entry = DebtEntry(
            id = 1L,
            personId = 1L,
            amount = amount,
            isOwedToMe = true,
            date = System.currentTimeMillis(),
            status = if (paid > 0) DebtStatus.PARTIAL else DebtStatus.OPEN
        )
        val payments = if (paid > 0) listOf(Payment(debtEntryId = 1L, amount = paid)) else emptyList()
        return DebtEntryWithPayments.from(entry, payments)
    }

    @Before
    fun setup() {
        paymentRepository = mockk()
        debtRepository = mockk()
        useCase = AddPartialPaymentUseCase(paymentRepository, debtRepository)
    }

    @Test
    fun `returns success for valid partial payment`() = runTest {
        val debt = makeOpenDebt(amount = 100.0)
        coEvery { debtRepository.getDebtEntryWithPayments(1L) } returns flowOf(debt)
        coEvery { paymentRepository.insertPaymentAndUpdateStatus(any(), "PARTIAL") } returns 1L

        val payment = Payment(debtEntryId = 1L, amount = 50.0)
        val result = useCase(payment)

        assertTrue(result is Result.Success)
        coVerify { paymentRepository.insertPaymentAndUpdateStatus(any(), "PARTIAL") }
    }

    @Test
    fun `sets status to PAID when payment covers full remaining`() = runTest {
        val debt = makeOpenDebt(amount = 100.0)
        coEvery { debtRepository.getDebtEntryWithPayments(1L) } returns flowOf(debt)
        coEvery { paymentRepository.insertPaymentAndUpdateStatus(any(), "PAID") } returns 1L

        val payment = Payment(debtEntryId = 1L, amount = 100.0)
        val result = useCase(payment)

        assertTrue(result is Result.Success)
        coVerify { paymentRepository.insertPaymentAndUpdateStatus(any(), "PAID") }
    }

    @Test
    fun `sets status to PAID when payment covers remaining after partial payments`() = runTest {
        val debt = makeOpenDebt(amount = 100.0, paid = 60.0)
        coEvery { debtRepository.getDebtEntryWithPayments(1L) } returns flowOf(debt)
        coEvery { paymentRepository.insertPaymentAndUpdateStatus(any(), "PAID") } returns 1L

        val payment = Payment(debtEntryId = 1L, amount = 40.0)
        val result = useCase(payment)

        assertTrue(result is Result.Success)
        coVerify { paymentRepository.insertPaymentAndUpdateStatus(any(), "PAID") }
    }

    @Test
    fun `returns error when payment exceeds remaining (L-034)`() = runTest {
        val debt = makeOpenDebt(amount = 100.0)
        coEvery { debtRepository.getDebtEntryWithPayments(1L) } returns flowOf(debt)

        val payment = Payment(debtEntryId = 1L, amount = 100.01)
        val result = useCase(payment)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("remaining", ignoreCase = true))
    }

    @Test
    fun `returns error when payment amount is zero`() = runTest {
        val payment = Payment(debtEntryId = 1L, amount = 0.0)
        val result = useCase(payment)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `returns error when payment amount is negative`() = runTest {
        val payment = Payment(debtEntryId = 1L, amount = -5.0)
        val result = useCase(payment)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `returns error when debt is already PAID`() = runTest {
        val entry = DebtEntry(
            id = 1L, personId = 1L, amount = 100.0, isOwedToMe = true,
            date = System.currentTimeMillis(), status = DebtStatus.PAID
        )
        val debt = DebtEntryWithPayments.from(entry, listOf(Payment(debtEntryId = 1L, amount = 100.0)))
        coEvery { debtRepository.getDebtEntryWithPayments(1L) } returns flowOf(debt)

        val payment = Payment(debtEntryId = 1L, amount = 10.0)
        val result = useCase(payment)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("paid", ignoreCase = true))
    }

    @Test
    fun `returns error when debt is CANCELLED`() = runTest {
        val entry = DebtEntry(
            id = 1L, personId = 1L, amount = 100.0, isOwedToMe = true,
            date = System.currentTimeMillis(), status = DebtStatus.CANCELLED
        )
        val debt = DebtEntryWithPayments.from(entry, emptyList())
        coEvery { debtRepository.getDebtEntryWithPayments(1L) } returns flowOf(debt)

        val payment = Payment(debtEntryId = 1L, amount = 10.0)
        val result = useCase(payment)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("cancelled", ignoreCase = true))
    }

    @Test
    fun `returns error when debt entry not found`() = runTest {
        coEvery { debtRepository.getDebtEntryWithPayments(99L) } returns flowOf(null)

        val payment = Payment(debtEntryId = 99L, amount = 10.0)
        val result = useCase(payment)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("not found", ignoreCase = true))
    }

    @Test
    fun `edge case - zero amount remaining after payment stays at 0`() = runTest {
        val debt = makeOpenDebt(amount = 100.0, paid = 0.0)
        coEvery { debtRepository.getDebtEntryWithPayments(1L) } returns flowOf(debt)
        coEvery { paymentRepository.insertPaymentAndUpdateStatus(any(), "PAID") } returns 1L

        // Exact match: payment == remaining
        val payment = Payment(debtEntryId = 1L, amount = 100.0)
        val result = useCase(payment)

        assertTrue(result is Result.Success)
    }
}
