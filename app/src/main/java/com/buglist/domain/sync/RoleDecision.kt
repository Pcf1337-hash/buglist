package com.buglist.domain.sync

/**
 * Result of comparing the two phones' role choices.
 * Exactly one host is allowed. Both host or both viewer stays unresolved.
 */
enum class RoleDecision {
    WAITING,
    BOTH_HOST,
    BOTH_VIEW,
    I_AM_HOST,
    I_AM_VIEWER
}

fun decideRoles(mineIsHost: Boolean?, peerIsHost: Boolean?): RoleDecision = when {
    mineIsHost == null || peerIsHost == null -> RoleDecision.WAITING
    mineIsHost && peerIsHost -> RoleDecision.BOTH_HOST
    !mineIsHost && !peerIsHost -> RoleDecision.BOTH_VIEW
    mineIsHost -> RoleDecision.I_AM_HOST
    else -> RoleDecision.I_AM_VIEWER
}
