package com.buglist.domain.model

/**
 * Status of a debt entry.
 *
 * Transitions:
 *   OPEN → PARTIAL  (first payment recorded, remaining > 0)
 *   OPEN → PAID     (full payment recorded in one shot)
 *   PARTIAL → PAID  (remaining payment covers the rest)
 *   OPEN | PARTIAL → CANCELLED  (user explicitly cancels)
 *
 * PAID and CANCELLED are terminal states.
 */
enum class DebtStatus {
    /** No payments recorded yet. */
    OPEN,

    /** At least one payment recorded but remaining > 0. */
    PARTIAL,

    /** Fully settled — remaining == 0. */
    PAID,

    /** Manually cancelled by the user. */
    CANCELLED;

    companion object {
        /**
         * Converts a stored String value back to [DebtStatus].
         * Defaults to [OPEN] for unknown values to avoid crashes on schema migration.
         */
        fun fromString(value: String): DebtStatus =
            entries.firstOrNull { it.name == value } ?: OPEN
    }
}
