package com.buglist.domain.model

/**
 * Who may change a coupled book.
 *
 * [HOST] publishes the book. The other phone is [GUEST] and only receives it.
 * [BOTH] means either phone may change the current book. There is no transaction log.
 */
enum class SyncRole {
    HOST,
    GUEST,
    BOTH;

    val canEdit: Boolean get() = this != GUEST

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
