package com.buglist.domain.model

/** One recorded payment, including payments on debts that are still open. */
data class RepaymentHistoryItem(
    val debt: DebtEntryWithPayments,
    val payment: Payment?
) {
    val key: String get() = "${debt.entry.id}:${payment?.id ?: "legacy"}"
    val amount: Double get() = payment?.amount ?: debt.entry.amount
    val date: Long? get() = payment?.date?.takeIf { it > 0L }
}

fun repaymentHistory(debts: List<DebtEntryWithPayments>): List<RepaymentHistoryItem> =
    debts.filter { it.entry.status != DebtStatus.CANCELLED }
        .flatMap { debt ->
            val payments = debt.payments.filter { it.amount > 0 && it.amount.isFinite() }
            if (payments.isEmpty() && debt.entry.status == DebtStatus.PAID) {
                // Preserve legacy paid marks without inventing a payment or its date.
                listOf(RepaymentHistoryItem(debt, null))
            } else {
                payments.map { RepaymentHistoryItem(debt, it) }
            }
        }
        .sortedWith(compareByDescending<RepaymentHistoryItem> { it.date ?: Long.MIN_VALUE }
            .thenByDescending { it.payment?.id ?: Long.MIN_VALUE }
            .thenByDescending { it.debt.entry.id })
