package com.buglist.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtEntryWithPaymentsTest {

    private fun makeEntry(amount: Double) = DebtEntry(
        id = 1L,
        personId = 1L,
        amount = amount,
        isOwedToMe = true,
        date = System.currentTimeMillis()
    )

    @Test
    fun `remaining is zero when no payments`() {
        val entry = makeEntry(100.0)
        val dwp = DebtEntryWithPayments.from(entry, emptyList())

        assertEquals(100.0, dwp.remaining, 0.001)
        assertEquals(0.0, dwp.totalPaid, 0.001)
    }

    @Test
    fun `remaining reduces correctly after partial payment`() {
        val entry = makeEntry(100.0)
        val payments = listOf(Payment(debtEntryId = 1L, amount = 40.0))
        val dwp = DebtEntryWithPayments.from(entry, payments)

        assertEquals(40.0, dwp.totalPaid, 0.001)
        assertEquals(60.0, dwp.remaining, 0.001)
    }

    @Test
    fun `remaining is zero when fully paid`() {
        val entry = makeEntry(100.0)
        val payments = listOf(
            Payment(debtEntryId = 1L, amount = 60.0),
            Payment(debtEntryId = 1L, amount = 40.0)
        )
        val dwp = DebtEntryWithPayments.from(entry, payments)

        assertEquals(100.0, dwp.totalPaid, 0.001)
        assertEquals(0.0, dwp.remaining, 0.001)
    }

    @Test
    fun `remaining is clamped to zero on floating-point overflow`() {
        val entry = makeEntry(100.0)
        // Simulate floating-point arithmetic giving slightly more than paid
        val payments = listOf(Payment(debtEntryId = 1L, amount = 100.00000001))
        val dwp = DebtEntryWithPayments.from(entry, payments)

        // Should not go negative
        assertTrue(dwp.remaining >= 0.0)
        assertEquals(0.0, dwp.remaining, 0.001)
    }

    @Test
    fun `sum of multiple small payments is correct`() {
        val entry = makeEntry(10.0)
        val payments = (1..10).map { Payment(debtEntryId = 1L, amount = 1.0) }
        val dwp = DebtEntryWithPayments.from(entry, payments)

        assertEquals(10.0, dwp.totalPaid, 0.001)
        assertEquals(0.0, dwp.remaining, 0.001)
    }

    @Test
    fun `zero amount debt edge case`() {
        val entry = makeEntry(0.0)
        val dwp = DebtEntryWithPayments.from(entry, emptyList())

        assertEquals(0.0, dwp.totalPaid, 0.001)
        assertEquals(0.0, dwp.remaining, 0.001)
    }
}
