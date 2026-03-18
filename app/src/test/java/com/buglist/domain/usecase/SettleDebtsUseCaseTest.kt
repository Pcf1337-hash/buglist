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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettleDebtsUseCaseTest {

    private lateinit var debtRepository: DebtRepository
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var useCase: SettleDebtsUseCase

    @Before
    fun setup() {
        debtRepository = mockk()
        paymentRepository = mockk()
        useCase = SettleDebtsUseCase(debtRepository, paymentRepository)
        // Default stub: insertPaymentAndUpdateStatus returns a dummy ID
        coEvery { paymentRepository.insertPaymentAndUpdateStatus(any(), any()) } returns 1L
    }

    /** Creates a [DebtEntryWithPayments] with the given amount and paid amount. */
    private fun makeDebt(
        id: Long,
        amount: Double,
        paid: Double = 0.0,
        isOwedToMe: Boolean = true,
        dateMs: Long = id * 1000L  // older IDs → earlier dates for FIFO testing
    ): DebtEntryWithPayments {
        val status = when {
            paid >= amount -> DebtStatus.PAID
            paid > 0 -> DebtStatus.PARTIAL
            else -> DebtStatus.OPEN
        }
        val entry = DebtEntry(
            id = id,
            personId = 1L,
            amount = amount,
            isOwedToMe = isOwedToMe,
            date = dateMs,
            status = status
        )
        val payments = if (paid > 0) listOf(Payment(debtEntryId = id, amount = paid)) else emptyList()
        return DebtEntryWithPayments.from(entry, payments)
    }

    // -------------------------------------------------------------------------
    // Test 1: Exactly the open amount → all entries become PAID
    // -------------------------------------------------------------------------
    @Test
    fun `exact total amount settles all entries as PAID`() = runTest {
        val debts = listOf(
            makeDebt(id = 1L, amount = 50.0, dateMs = 1000L),
            makeDebt(id = 2L, amount = 80.0, paid = 20.0, dateMs = 2000L),  // remaining = 60
            makeDebt(id = 3L, amount = 30.0, dateMs = 3000L)
        )
        // Total remaining = 50 + 60 + 30 = 140
        coEvery { debtRepository.getOpenDebtsForPerson(1L, true) } returns debts

        val result = useCase(personId = 1L, totalAmount = 140.0, isOwedToMe = true)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(3, data.settledEntries.size)
        assertNull(data.partialEntry)
        assertEquals(140.0, data.totalSettled, 0.01)
        assertEquals(0.0, data.remainingBudget, 0.01)
        data.settledEntries.forEach { assertEquals(DebtStatus.PAID, it.newStatus) }
    }

    // -------------------------------------------------------------------------
    // Test 2: Amount < first entry remaining → first entry becomes PARTIAL
    // -------------------------------------------------------------------------
    @Test
    fun `amount less than first entry remaining makes it PARTIAL`() = runTest {
        val debts = listOf(
            makeDebt(id = 1L, amount = 100.0, dateMs = 1000L),
            makeDebt(id = 2L, amount = 50.0, dateMs = 2000L)
        )
        coEvery { debtRepository.getOpenDebtsForPerson(1L, true) } returns debts

        val result = useCase(personId = 1L, totalAmount = 40.0, isOwedToMe = true)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertTrue(data.settledEntries.isEmpty())
        val partial = data.partialEntry
        assertTrue(partial != null)
        assertEquals(1L, partial!!.debtEntry.id)
        assertEquals(DebtStatus.PARTIAL, partial.newStatus)
        assertEquals(40.0, partial.amountPaid, 0.01)
        assertEquals(0.0, data.remainingBudget, 0.01)
    }

    // -------------------------------------------------------------------------
    // Test 3: Amount > all open debts → all PAID, remainingBudget > 0
    // -------------------------------------------------------------------------
    @Test
    fun `amount exceeding all debts leaves remaining budget`() = runTest {
        val debts = listOf(
            makeDebt(id = 1L, amount = 50.0, dateMs = 1000L),
            makeDebt(id = 2L, amount = 30.0, dateMs = 2000L)
        )
        coEvery { debtRepository.getOpenDebtsForPerson(1L, true) } returns debts

        val result = useCase(personId = 1L, totalAmount = 200.0, isOwedToMe = true)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.settledEntries.size)
        assertNull(data.partialEntry)
        assertEquals(80.0, data.totalSettled, 0.01)
        assertEquals(120.0, data.remainingBudget, 0.01)
    }

    // -------------------------------------------------------------------------
    // Test 4: No open debts → empty result, no DB transaction
    // -------------------------------------------------------------------------
    @Test
    fun `no open debts returns empty result without touching the DB`() = runTest {
        coEvery { debtRepository.getOpenDebtsForPerson(1L, true) } returns emptyList()

        val result = useCase(personId = 1L, totalAmount = 50.0, isOwedToMe = true)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertTrue(data.settledEntries.isEmpty())
        assertNull(data.partialEntry)
        assertEquals(0.0, data.totalSettled, 0.01)
        assertEquals(50.0, data.remainingBudget, 0.01)

        // No payment should have been inserted
        coVerify(exactly = 0) { paymentRepository.insertPaymentAndUpdateStatus(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // Test 5: FIFO order verified — oldest (lowest dateMs) settled first
    // -------------------------------------------------------------------------
    @Test
    fun `FIFO order settles oldest entry first`() = runTest {
        // Deliberately insert in reversed chronological order — DAO returns ASC by date
        val debts = listOf(
            makeDebt(id = 10L, amount = 50.0, dateMs = 1000L),  // oldest
            makeDebt(id = 20L, amount = 50.0, dateMs = 9000L)   // newest
        )
        coEvery { debtRepository.getOpenDebtsForPerson(1L, true) } returns debts

        val result = useCase(personId = 1L, totalAmount = 50.0, isOwedToMe = true)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.settledEntries.size)
        assertEquals(10L, data.settledEntries.first().debtEntry.id)  // oldest settled
        assertEquals(DebtStatus.PAID, data.settledEntries.first().newStatus)
        assertNull(data.partialEntry)
    }

    // -------------------------------------------------------------------------
    // Validation: invalid personId
    // -------------------------------------------------------------------------
    @Test
    fun `returns error for invalid person ID`() = runTest {
        val result = useCase(personId = 0L, totalAmount = 100.0, isOwedToMe = true)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Invalid", ignoreCase = true))
    }

    // -------------------------------------------------------------------------
    // Validation: zero or negative amount
    // -------------------------------------------------------------------------
    @Test
    fun `returns error for zero amount`() = runTest {
        val result = useCase(personId = 1L, totalAmount = 0.0, isOwedToMe = true)
        assertTrue(result is Result.Error)
    }

    @Test
    fun `returns error for negative amount`() = runTest {
        val result = useCase(personId = 1L, totalAmount = -10.0, isOwedToMe = true)
        assertTrue(result is Result.Error)
    }

    // -------------------------------------------------------------------------
    // FIFO with PARTIAL entry — partial entry's remaining is used, not amount
    // -------------------------------------------------------------------------
    @Test
    fun `uses remaining (not amount) for PARTIAL entries in FIFO`() = runTest {
        // Entry B already has 20€ paid, so remaining = 60€
        val debts = listOf(
            makeDebt(id = 1L, amount = 50.0, dateMs = 1000L),
            makeDebt(id = 2L, amount = 80.0, paid = 20.0, dateMs = 2000L),  // remaining = 60
            makeDebt(id = 3L, amount = 30.0, dateMs = 3000L)
        )
        coEvery { debtRepository.getOpenDebtsForPerson(1L, true) } returns debts

        // Settle exactly 110€: entry 1 (50) + 60 from entry 2 = 110, entry 2 fully paid
        val result = useCase(personId = 1L, totalAmount = 110.0, isOwedToMe = true)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.settledEntries.size)
        assertEquals(1L, data.settledEntries[0].debtEntry.id)
        assertEquals(50.0, data.settledEntries[0].amountPaid, 0.01)
        assertEquals(2L, data.settledEntries[1].debtEntry.id)
        assertEquals(60.0, data.settledEntries[1].amountPaid, 0.01)
        assertNull(data.partialEntry)
        // Entry 3 untouched
        assertEquals(0.0, data.remainingBudget, 0.01)
    }
}
