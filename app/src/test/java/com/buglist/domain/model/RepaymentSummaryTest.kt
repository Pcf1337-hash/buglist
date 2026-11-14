package com.buglist.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RepaymentSummaryTest {
    private fun debt(amount: Double, status: DebtStatus = DebtStatus.PARTIAL,
        currency: String = "EUR", owedToMe: Boolean = true, vararg paid: Double) =
        DebtEntryWithPayments.from(
            DebtEntry(personId = 1, amount = amount, date = 1, status = status,
                currency = currency, isOwedToMe = owedToMe),
            paid.mapIndexed { index, value -> Payment(id = index.toLong() + 1,
                debtEntryId = 1, amount = value, date = index.toLong() + 1) }
        )

    @Test fun rateUsesMoneyIncludingPartialPaymentsRatherThanPaidEntryCount() {
        val total = repaymentSummaries(listOf(
            debt(100.0, paid = doubleArrayOf(20.0, 50.0)),
            debt(50.0, DebtStatus.PAID, paid = doubleArrayOf(50.0))
        )).single()
        assertEquals(120.0, total.repaid, 0.001)
        assertEquals(150.0, total.original, 0.001)
        assertEquals(0.8f, total.fraction, 0.001f)
    }

    @Test fun currenciesAndDirectionsNeverNetAndCancelledDebtsAreExcluded() {
        val totals = repaymentSummaries(listOf(
            debt(100.0, paid = doubleArrayOf(20.0)),
            debt(100.0, currency = "USD", paid = doubleArrayOf(50.0)),
            debt(100.0, owedToMe = false, paid = doubleArrayOf(40.0)),
            debt(900.0, DebtStatus.CANCELLED, paid = doubleArrayOf(900.0))
        ))
        assertEquals(3, totals.size)
        assertEquals(20.0, totals.first { it.currency == "EUR" && it.isOwedToMe }.repaid, 0.001)
        assertEquals(40.0, totals.first { !it.isOwedToMe }.repaid, 0.001)
        assertEquals(50.0, totals.first { it.currency == "USD" }.repaid, 0.001)
    }

    @Test fun legacyPaidMarksCountAndCentsAndOverpaymentsStayBounded() {
        val total = repaymentSummaries(listOf(
            debt(0.3, DebtStatus.PAID),
            debt(0.3, paid = doubleArrayOf(0.1, 0.2)),
            debt(1.0, paid = doubleArrayOf(2.0))
        )).single()
        assertEquals(1.6, total.repaid, 0.00001)
        assertEquals(1f, total.fraction, 0.00001f)
        assertEquals(emptyList<RepaymentSummary>(), repaymentSummaries(emptyList()))
    }
}
