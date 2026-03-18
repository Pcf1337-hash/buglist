package com.buglist.domain.model

/**
 * Result of a debt settlement operation performed by [com.buglist.domain.usecase.SettleDebtsUseCase].
 *
 * @param settledEntries  Debt entries that were fully settled (status → PAID).
 * @param partialEntry    The last debt entry that was partially settled, if any.
 *                        This is the entry that received the final fragment of the budget.
 * @param totalSettled    The actual amount applied across all entries.
 *                        May be less than the input amount if debts total less than the budget.
 * @param remainingBudget How much of the input budget was NOT consumed (budget > all open debts).
 */
data class SettlementResult(
    val settledEntries: List<SettledEntry>,
    val partialEntry: SettledEntry?,
    val totalSettled: Double,
    val remainingBudget: Double
)

/**
 * Summary of how a single [DebtEntry] was affected by a settlement operation.
 *
 * @param debtEntry    The debt entry that was touched.
 * @param amountPaid   How much was applied to this entry in this settlement run.
 * @param newStatus    The status of the entry after the settlement.
 */
data class SettledEntry(
    val debtEntry: DebtEntry,
    val amountPaid: Double,
    val newStatus: DebtStatus
)
