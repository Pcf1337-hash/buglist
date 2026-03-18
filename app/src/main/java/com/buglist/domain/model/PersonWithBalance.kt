package com.buglist.domain.model

/**
 * Computed aggregate view of a [Person] combined with their financial summary.
 *
 * Used in the dashboard list to show each contact's outstanding balance at a glance.
 *
 * @param person      The underlying person record.
 * @param netBalance  Net outstanding amount.
 *                    Positive  = the person owes the user (sum of their OPEN/PARTIAL debts owed to me).
 *                    Negative  = the user owes this person.
 *                    Based on **remaining** amounts (original minus payments), not originals.
 * @param openCount   Number of debt entries in OPEN or PARTIAL state.
 */
data class PersonWithBalance(
    val person: Person,
    val netBalance: Double,
    val openCount: Int
)
