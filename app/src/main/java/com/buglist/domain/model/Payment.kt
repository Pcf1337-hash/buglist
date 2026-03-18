package com.buglist.domain.model

/**
 * Domain model representing a partial (or full) payment on a [DebtEntry].
 *
 * Each payment reduces the remaining balance of its parent [DebtEntry].
 * When the sum of payments equals [DebtEntry.amount], the debt is fully [DebtStatus.PAID].
 *
 * @param id           Auto-generated primary key (0 = not yet persisted).
 * @param debtEntryId  FK reference to [DebtEntry.id]. CASCADE DELETE from the parent.
 * @param amount       Payment amount — always positive and <= remaining at time of recording.
 * @param note         Optional note for this payment (e.g., "Cash" / "Bank transfer").
 * @param date         When this payment was made (Unix ms).
 */
data class Payment(
    val id: Long = 0,
    val debtEntryId: Long,
    val amount: Double,
    val note: String? = null,
    val date: Long = System.currentTimeMillis()
)
