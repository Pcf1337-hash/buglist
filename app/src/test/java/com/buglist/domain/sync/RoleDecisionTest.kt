package com.buglist.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class RoleDecisionTest {
    @Test
    fun waitsUntilBothHaveChosen() {
        assertEquals(RoleDecision.WAITING, decideRoles(true, null))
        assertEquals(RoleDecision.WAITING, decideRoles(null, false))
    }

    @Test
    fun rejectsTwoHostsAndTwoViewers() {
        assertEquals(RoleDecision.BOTH_HOST, decideRoles(true, true))
        assertEquals(RoleDecision.BOTH_VIEW, decideRoles(false, false))
    }

    @Test
    fun acceptsOneHost() {
        assertEquals(RoleDecision.I_AM_HOST, decideRoles(true, false))
        assertEquals(RoleDecision.I_AM_VIEWER, decideRoles(false, true))
    }
}
