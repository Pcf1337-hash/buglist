package com.buglist.domain.model

/**
 * Computed view of a [DebtEntry] combined with all its [Payment] records.
 *
 * [totalPaid] and [remaining] are derived fields — they are NOT stored in the
 * database but computed on every read to guarantee consistency.
 *
 * @param entry      The underlying debt entry.
 * @param payments   All payment records for this entry, ordered newest-first.
 * @param totalPaid  Sum of all [Payment.amount] values.
 * @param remaining  [DebtEntry.amount] minus [totalPaid]. Never negative.
 */
data class DebtEntryWithPayments(
    val entry: DebtEntry,
    val payments: List<Payment>,
    val totalPaid: Double,
    val remaining: Double
) {
    companion object {
        /**
         * Creates a [DebtEntryWithPayments] from a [DebtEntry] and its payments,
         * automatically computing [totalPaid] and [remaining].
         *
         * [remaining] is clamped to 0.0 to guard against floating-point drift.
         */
        fun from(entry: DebtEntry, payments: List<Payment>): DebtEntryWithPayments {
            val totalPaid = payments.sumOf { it.amount }
            val remaining = maxOf(0.0, entry.amount - totalPaid)
            return DebtEntryWithPayments(
                entry = entry,
                payments = payments,
                totalPaid = totalPaid,
                remaining = remaining
            )
        }
    }
}
