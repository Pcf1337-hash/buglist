package com.buglist.domain.model

/**
 * Domain model representing a single debt record between the app user and a [Person].
 *
 * The [amount] always stores the **original** debt value (always positive).
 * Use [DebtEntryWithPayments.remaining] for the currently outstanding amount.
 *
 * @param id          Auto-generated primary key (0 = not yet persisted).
 * @param personId    FK reference to [Person.id].
 * @param amount      Original debt amount — always positive.
 * @param currency    ISO 4217 currency code (default EUR).
 * @param isOwedToMe  true = the person owes the user; false = the user owes the person.
 * @param description Optional human-readable note about the debt.
 * @param date        Date the debt was incurred (Unix ms).
 * @param dueDate     Optional due date (Unix ms).
 * @param status      Current [DebtStatus] — auto-managed by [PaymentRepository].
 * @param createdAt   Record creation timestamp (Unix ms).
 * @param tags        Tag names attached to this entry (denormalised from tag cross-ref table).
 *                    Empty for entries loaded without tag enrichment.
 */
data class DebtEntry(
    val id: Long = 0,
    val personId: Long,
    val amount: Double,
    val currency: String = "EUR",
    val isOwedToMe: Boolean,
    val description: String? = null,
    val date: Long,
    val dueDate: Long? = null,
    val status: DebtStatus = DebtStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
)
