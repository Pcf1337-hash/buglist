package com.buglist.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the authentication session state for BugList.
 *
 * Tracks whether the user is currently authenticated. The session is locked
 * explicitly when the user navigates away from the app (via [MainActivity.onUserLeaveHint]),
 * which also calls [finishAndRemoveTask] to ensure a fresh biometric prompt on
 * every subsequent launch.
 *
 * All protected routes in [BugListNavHost] observe [isAuthenticated] and redirect
 * to the auth screen when it is false.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _isAuthenticated = MutableStateFlow(false)

    /**
     * Observed by [BugListNavHost]. When this drops to false, the user is redirected
     * to the auth screen and all sensitive data is no longer accessible.
     */
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /**
     * Called by [BugListNavHost] immediately after successful biometric authentication.
     */
    fun onAuthenticated() {
        _isAuthenticated.value = true
    }

    /**
     * Locks the session. Called from [MainActivity.onUserLeaveHint] when the user
     * navigates away, or after all data has been deleted.
     */
    fun lock() {
        _isAuthenticated.value = false
    }
}
