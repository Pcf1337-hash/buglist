package com.buglist.domain.model

import kotlin.math.roundToLong

data class RepaymentSummary(
    val currency: String,
    val isOwedToMe: Boolean,
    val repaid: Double,
    val original: Double
) {
    val fraction: Float get() = if (original > 0) (repaid / original).coerceIn(0.0, 1.0).toFloat() else 0f
}

/** All-time repayment by value, with currencies and payment directions kept separate. */
fun repaymentSummaries(debts: List<DebtEntryWithPayments>): List<RepaymentSummary> =
    debts.filter { it.entry.status != DebtStatus.CANCELLED && it.entry.amount.isFinite() && it.entry.amount > 0 }
        .groupBy { it.entry.currency to it.entry.isOwedToMe }
        .map { (key, entries) ->
            var originalCents = 0L
            var repaidCents = 0L
            entries.forEach { debt ->
                val original = (debt.entry.amount * 100).roundToLong()
                val payments = debt.payments.filter { it.amount.isFinite() && it.amount > 0 }
                val paid = if (payments.isEmpty() && debt.entry.status == DebtStatus.PAID) original
                    else payments.sumOf { (it.amount * 100).roundToLong() }.coerceIn(0, original)
                originalCents += original
                repaidCents += paid
            }
            RepaymentSummary(key.first, key.second, repaidCents / 100.0, originalCents / 100.0)
        }
        .sortedWith(compareBy<RepaymentSummary> { it.currency }.thenByDescending { it.isOwedToMe })
