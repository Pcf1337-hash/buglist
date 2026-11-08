package com.buglist.domain.model

/**
 * Who may change a coupled book.
 *
 * [HOST] keeps the list and publishes it.
 * [GUEST] only receives that list. Editing stays locked until sync is turned off.
 * [BOTH] is no longer offered. Stored rows are receive-only.
 */
enum class SyncRole {
    HOST,
    GUEST,
    BOTH;

    val canEdit: Boolean get() = this == HOST

    companion object {
        fun fromStored(value: String): SyncRole = when (value) {
            "GUEST" -> GUEST
            "BOTH" -> BOTH
            else -> HOST
        }
    }
}

/** What the person screen needs to know about a coupling. The key never leaves the data layer. */
data class PairingLink(
    val role: SyncRole
)
