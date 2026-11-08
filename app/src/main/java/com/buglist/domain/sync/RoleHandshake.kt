package com.buglist.domain.sync

/**
 * One message on the shared mailbox. [kind] is "hello", "role" or "book".
 * The ciphertext stays outside this type. Two phones run [handshake] on the
 * same list, oldest first.
 */
data class PeerMessage(
    val from: String,
    val kind: String,
    val role: String? = null
)

data class PhoneSync(
    val deviceId: String,
    val wanted: String? = null,
    val role: String = "PENDING",
    val peerRole: String? = null,
    val peerSeen: Boolean = false
)

enum class SyncProblem {
    BOTH_HOST,
    BOTH_VIEW,
    NO_BOOK,
    OFFLINE
}

data class HandshakeResult(
    val phone: PhoneSync,
    val outgoing: List<PeerMessage>,
    val pushBook: Boolean,
    val takeBook: Boolean,
    val problem: SyncProblem?
)

/**
 * A fetch that failed is never reported as an empty book.
 * The caller maps [SyncProblem] to the short German line.
 */
fun visibleProblem(fetchFailed: Boolean, problem: SyncProblem?): SyncProblem? =
    if (fetchFailed) SyncProblem.OFFLINE else problem

/**
 * Decide this phone's role from the mailbox and say what it must publish.
 * The chosen role is published even after it locks, so the other phone can
 * leave "pending". The latest role message from the peer wins.
 */
fun handshake(phone: PhoneSync, inbox: List<PeerMessage>): HandshakeResult {
    var peerRole = phone.peerRole
    var peerSeen = phone.peerSeen
    var bookFromPeer = false
    for (message in inbox) {
        if (message.from.isBlank() || message.from == phone.deviceId) continue
        peerSeen = true
        when (message.kind) {
            "role" -> peerRole = if (message.role == "HOST") "HOST" else "GUEST"
            "book" -> bookFromPeer = true
            else -> Unit
        }
    }
    val decision = decideRoles(
        mineIsHost = phone.wanted?.let { it == "HOST" },
        peerIsHost = peerRole?.let { it == "HOST" }
    )
    val role = when (decision) {
        RoleDecision.I_AM_HOST -> "HOST"
        RoleDecision.I_AM_VIEWER -> "GUEST"
        else -> "PENDING"
    }
    val outgoing = buildList {
        if (!peerSeen) add(PeerMessage(phone.deviceId, "hello"))
        val wanted = phone.wanted
        if (wanted != null) add(PeerMessage(phone.deviceId, "role", wanted))
    }
    val conflict = decision == RoleDecision.BOTH_HOST || decision == RoleDecision.BOTH_VIEW
    val problem = when (decision) {
        RoleDecision.BOTH_HOST -> SyncProblem.BOTH_HOST
        RoleDecision.BOTH_VIEW -> SyncProblem.BOTH_VIEW
        RoleDecision.I_AM_VIEWER -> if (bookFromPeer) null else SyncProblem.NO_BOOK
        else -> null
    }
    return HandshakeResult(
        phone = phone.copy(role = role, peerRole = peerRole, peerSeen = peerSeen),
        outgoing = outgoing,
        pushBook = phone.wanted == "HOST" && !conflict,
        takeBook = role == "GUEST" && bookFromPeer,
        problem = problem
    )
}
