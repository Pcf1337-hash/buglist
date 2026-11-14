package com.buglist.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepaymentHistoryTest {
    private fun debt(id: Long, status: DebtStatus, vararg payments: Payment) =
        DebtEntryWithPayments.from(
            DebtEntry(id = id, personId = 1, amount = 100.0, isOwedToMe = id == 1L,
                date = 10L, status = status),
            payments.toList()
        )

    @Test fun individualPaymentsAppearNewestFirstIncludingPartialDebts() {
        val partial = debt(1, DebtStatus.PARTIAL,
            Payment(id = 1, debtEntryId = 1, amount = 20.0, date = 100L),
            Payment(id = 2, debtEntryId = 1, amount = 50.0, date = 200L))
        val paid = debt(2, DebtStatus.PAID, Payment(id = 3, debtEntryId = 2, amount = 100.0, date = 150L))
        val rows = repaymentHistory(listOf(partial, paid))

        assertEquals(listOf(50.0, 100.0, 20.0), rows.map { it.amount })
        assertEquals(listOf(200L, 150L, 100L), rows.map { it.date })
        assertEquals(listOf(true, false, true), rows.map { it.debt.entry.isOwedToMe })
        assertEquals(3, rows.map { it.key }.toSet().size)
    }

    @Test fun paidDebtDoesNotAddOriginalAmountOnTopOfItsPayments() {
        val paid = debt(1, DebtStatus.PAID,
            Payment(id = 1, debtEntryId = 1, amount = 60.0, date = 100L),
            Payment(id = 2, debtEntryId = 1, amount = 40.0, date = 200L))
        assertEquals(100.0, repaymentHistory(listOf(paid)).sumOf { it.amount }, 0.001)
    }

    @Test fun legacyPaidMarkIsPreservedAsAnUndatedMarkRatherThanAnInventedPayment() {
        val rows = repaymentHistory(listOf(debt(1, DebtStatus.PAID), debt(2, DebtStatus.OPEN)))
        assertEquals(1, rows.size)
        assertEquals(100.0, rows.single().amount, 0.001)
        assertNull(rows.single().date)
        assertNull(rows.single().payment)
    }

    @Test fun cancelledDebtsAreExcludedAndSameTimestampsDoNotMergeSeparatePayments() {
        val one = Payment(id = 1, debtEntryId = 1, amount = 30.0, date = 100L)
        val two = Payment(id = 2, debtEntryId = 1, amount = 20.0, date = 100L)
        val rows = repaymentHistory(listOf(
            debt(1, DebtStatus.PARTIAL, one, two),
            debt(2, DebtStatus.CANCELLED, Payment(id = 3, debtEntryId = 2, amount = 80.0, date = 200L))))
        assertEquals(2, rows.size)
        assertNotEquals(rows[0].key, rows[1].key)
        assertEquals(50.0, rows.sumOf { it.amount }, 0.001)
    }
}
