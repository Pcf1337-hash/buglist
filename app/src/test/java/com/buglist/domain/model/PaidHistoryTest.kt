package com.buglist.domain.model

import org.junit.Assert.*
import org.junit.Test

class PaidHistoryTest {
    private fun debt(id: Long = 1, date: Long = 10, amount: Double = 100.0,
                     status: DebtStatus = DebtStatus.PAID, payments: List<Payment> = emptyList()) =
        DebtEntryWithPayments.from(DebtEntry(id = id, personId = 1, amount = amount,
            isOwedToMe = true, date = date, status = status), payments)

    private fun payment(amount: Double, date: Long, id: Long = date) =
        Payment(id = id, debtEntryId = 1, amount = amount, date = date)

    @Test fun completionComesFromThePaymentThatClearsTheDebt() {
        val debt = debt(payments = listOf(payment(40.0, 300), payment(60.0, 200)))
        assertEquals(300L, debt.paidAt)
        assertEquals(listOf(40.0, 0.0), debt.paymentProgress().map { it.remaining })
        assertEquals(listOf(false, true), debt.paymentProgress().map { it.completesDebt })
    }

    @Test fun recentlyPaidOldDebtComesBeforeAnEarlierPaidNewDebt() {
        val old = debt(id = 1, date = 1, payments = listOf(payment(100.0, 500)))
        val recent = debt(id = 2, date = 200, payments = listOf(payment(100.0, 300)))
        assertEquals(listOf(1L, 2L), paidHistory(listOf(recent, old)).map { it.entry.id })
    }

    @Test fun cancelledAndPartiallyPaidDebtsDoNotAppearInPaidHistory() {
        val cancelled = debt(status = DebtStatus.CANCELLED, payments = listOf(payment(100.0, 300)))
        val partial = debt(status = DebtStatus.PARTIAL, payments = listOf(payment(20.0, 200)))
        assertTrue(paidHistory(listOf(cancelled, partial)).isEmpty())
        assertNull(cancelled.paidAt)
        assertNull(partial.paidAt)
    }

    @Test fun missingPaymentRecordsNeverUseTheOriginalDebtDate() {
        assertNull(debt(date = 123456789).paidAt)
        assertNull(debt(payments = listOf(payment(50.0, 300))).paidAt)
        assertNull(debt(payments = listOf(payment(100.0, 0))).paidAt)
    }

    @Test fun unknownCompletionDatesComeLast() {
        val unknown = debt(id = 1, date = 9999)
        val known = debt(id = 2, payments = listOf(payment(100.0, 300)))
        assertEquals(listOf(2L, 1L), paidHistory(listOf(unknown, known)).map { it.entry.id })
    }

    @Test fun decimalPaymentsClearTheBalanceExactlyInCents() {
        val debt = debt(amount = 0.3, payments = listOf(payment(0.1, 100), payment(0.2, 200)))
        assertEquals(200L, debt.paidAt)
        assertEquals(0.0, debt.paymentProgress().last().remaining, 0.0)
    }

    @Test fun laterExtraRecordDoesNotChangeTheCompletionDate() {
        val debt = debt(payments = listOf(payment(100.0, 200), payment(10.0, 300)))
        assertEquals(200L, debt.paidAt)
        assertEquals(1, debt.paymentProgress().count { it.completesDebt })
    }

    @Test fun simultaneousPaymentsHaveStableOrder() {
        val debt = debt(payments = listOf(payment(40.0, 200, 2), payment(60.0, 200, 1)))
        assertEquals(listOf(1L, 2L), debt.paymentProgress().map { it.payment.id })
        assertEquals(listOf(40.0, 0.0), debt.paymentProgress().map { it.remaining })
    }
}
