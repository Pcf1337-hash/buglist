package com.buglist.presentation.person_detail

import com.buglist.domain.model.PairingLink
import com.buglist.domain.model.SyncRole
import org.junit.Assert.assertEquals
import org.junit.Test

class CouplingWizardStateTest {
    private fun link(role: SyncRole = SyncRole.PENDING, mine: Boolean? = null,
                     peer: Boolean? = null, seen: Boolean = true) = PairingLink(role, mine, peer, seen)

    @Test fun noConnectionStartsWithCodeExchange() {
        assertEquals(CouplingStep.CONNECT, couplingStep(null, false, false))
    }
    @Test fun codeAloneDoesNotSkipToRoleSelection() {
        assertEquals(CouplingStep.CONNECT, couplingStep(link(seen = false), true, false))
    }
    @Test fun bothPhonesMustConfirmTheirRoles() {
        assertEquals(CouplingStep.ROLE, couplingStep(link(), false, false))
        assertEquals(CouplingStep.ROLE, couplingStep(link(mine = true), true, false))
        assertEquals(CouplingStep.ROLE, couplingStep(link(peer = false), true, false))
    }
    @Test fun twoHostsCannotCompleteTheWizard() {
        assertEquals(CouplingStep.ROLE, couplingStep(link(SyncRole.HOST, true, true), true, false))
    }
    @Test fun twoViewersCannotCompleteTheWizard() {
        assertEquals(CouplingStep.ROLE, couplingStep(link(SyncRole.GUEST, false, false), true, false))
    }
    @Test fun confirmedRolesStillNeedASuccessfulSync() {
        assertEquals(CouplingStep.VERIFY, couplingStep(link(SyncRole.GUEST, false, true), false, false))
    }
    @Test fun runningCheckDoesNotDisplayStaleSuccess() {
        assertEquals(CouplingStep.VERIFY, couplingStep(link(SyncRole.HOST, true, false), true, true))
    }
    @Test fun complementaryRolesAndSuccessfulSyncFinishOnBothPhones() {
        assertEquals(CouplingStep.DONE, couplingStep(link(SyncRole.HOST, true, false), true, false))
        assertEquals(CouplingStep.DONE, couplingStep(link(SyncRole.GUEST, false, true), true, false))
    }
}
