package com.buglist.security

import org.junit.Assert.*
import org.junit.Test

class PermissionLeaveGuardTest {
    @Test fun normalHomeAlwaysLocks() {
        assertTrue(PermissionLeaveGuard().lockOnLeaveHint())
    }
    @Test fun permissionDialogDefersTheLeaveHint() {
        val guard = PermissionLeaveGuard()
        guard.beginRequest()
        assertFalse(guard.lockOnLeaveHint())
    }
    @Test fun homeDuringPermissionDialogStillLocksOnStop() {
        val guard = PermissionLeaveGuard()
        guard.beginRequest()
        guard.lockOnLeaveHint()
        assertTrue(guard.lockOnStop(false))
    }
    @Test fun receivingPermissionResultDoesNotHideARealLeave() {
        val guard = PermissionLeaveGuard()
        guard.beginRequest()
        guard.lockOnLeaveHint()
        guard.endRequest()
        assertTrue(guard.lockOnStop(false))
    }
    @Test fun resumingFromPermissionDialogRestoresNormalHomeLock() {
        val guard = PermissionLeaveGuard()
        guard.beginRequest()
        guard.lockOnLeaveHint()
        guard.endRequest()
        guard.onResumed()
        assertFalse(guard.lockOnStop(false))
        assertTrue(guard.lockOnLeaveHint())
    }
    @Test fun configurationChangeIsNotAUserLeave() {
        val guard = PermissionLeaveGuard()
        guard.beginRequest()
        guard.lockOnLeaveHint()
        assertFalse(guard.lockOnStop(true))
    }
}
