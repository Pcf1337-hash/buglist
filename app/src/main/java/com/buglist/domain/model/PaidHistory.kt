package com.buglist.domain.model

import kotlin.math.roundToLong

data class PaymentProgress(
    val payment: Payment,
    val remaining: Double,
    val completesDebt: Boolean
)

/** Chronological payments with cent-exact remaining amounts, independent of DAO order. */
fun DebtEntryWithPayments.paymentProgress(): List<PaymentProgress> {
    val original = (entry.amount * 100).roundToLong()
    var paid = 0L
    return payments.sortedWith(compareBy<Payment> { it.date }.thenBy { it.id }).map { payment ->
        val before = paid
        paid += (payment.amount * 100).roundToLong()
        PaymentProgress(
            payment = payment,
            remaining = (original - paid).coerceAtLeast(0) / 100.0,
            completesDebt = entry.status == DebtStatus.PAID && original > 0 && before < original && paid >= original
        )
    }
}

/** The payment that actually cleared the debt. Never substitute the debt's creation date. */
val DebtEntryWithPayments.paidAt: Long?
    get() = if (entry.status == DebtStatus.PAID) {
        paymentProgress().firstOrNull { it.completesDebt }?.payment?.date?.takeIf { it > 0 }
    } else null

/** Cancelled rows are not payments. Undated legacy rows stay at the end. */
fun paidHistory(debts: List<DebtEntryWithPayments>): List<DebtEntryWithPayments> = debts
    .filter { it.entry.status == DebtStatus.PAID }
    .sortedWith(compareByDescending<DebtEntryWithPayments> { it.paidAt ?: Long.MIN_VALUE }
        .thenByDescending { it.entry.date }.thenByDescending { it.entry.id })
