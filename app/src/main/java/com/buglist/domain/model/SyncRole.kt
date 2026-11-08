package com.buglist.domain.model

/**
 * Who may change a coupled book.
 *
 * [HOST] keeps the list and publishes it.
 * [GUEST] only receives that list. Editing stays locked until sync is turned off.
 * [BOTH] is no longer offered. Stored rows are receive-only.
 */
enum class SyncRole {
    /** Code is exchanged, the role has not been agreed yet. */
    PENDING,
    HOST,
    GUEST,
    BOTH;

    /** Viewer phones stay locked. Pending phones can still edit until a role is agreed. */
    val canEdit: Boolean get() = this == HOST || this == PENDING

    companion object {
        fun fromStored(value: String): SyncRole = when (value) {
            "PENDING" -> PENDING
            "GUEST" -> GUEST
            "BOTH" -> GUEST
            else -> HOST
        }
    }
}

/** What the person screen needs to know about a coupling. The key never leaves the data layer. */
data class PairingLink(
    val role: SyncRole,
    val wantedHost: Boolean?,
    val peerHost: Boolean?,
    val peerSeen: Boolean
)
