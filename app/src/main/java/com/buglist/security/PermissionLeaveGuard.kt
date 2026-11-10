package com.buglist.security

/** A permission dialog may send onUserLeaveHint even though the app remains visible. */
class PermissionLeaveGuard {
    private var requesting = false
    private var deferredLeave = false

    fun beginRequest() { requesting = true }
    fun endRequest() { requesting = false }
    fun onResumed() { deferredLeave = false }

    fun lockOnLeaveHint(): Boolean {
        if (!requesting) return true
        deferredLeave = true
        return false
    }

    // Home while a permission dialog is open still hides the activity and must lock it.
    fun lockOnStop(changingConfiguration: Boolean): Boolean = deferredLeave && !changingConfiguration
}
