package com.buglist.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the authentication session lifecycle for BugList.
 *
 * Tracks whether the user is currently authenticated and enforces auto-lock
 * when the app goes to background for longer than [autoLockTimeoutMs].
 *
 * Observes [ProcessLifecycleOwner] (i.e. the entire app process, not individual activities)
 * so that the lock timer fires only when the whole app is backgrounded — not when the
 * user switches between screens or rotates the device.
 *
 * Security guarantee: [isAuthenticated] becomes false after [autoLockTimeoutMs] in
 * background. All protected routes in [BugListNavHost] observe this and redirect to auth.
 *
 * @param autoLockTimeoutMs  Auto-lock delay in milliseconds. 0 = lock immediately on stop.
 */
@Singleton
class SessionManager @Inject constructor() : DefaultLifecycleObserver {

    companion object {
        /** Default auto-lock timeout: 5 minutes. Overridable from Settings. */
        const val DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L
    }

    private val _isAuthenticated = MutableStateFlow(false)

    /**
     * Observed by [BugListNavHost]. When this drops to false, the user is redirected to
     * the auth screen and all sensitive data is no longer accessible.
     */
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /** Configurable timeout — updated from SettingsViewModel when user changes preference. */
    @Volatile
    var autoLockTimeoutMs: Long = DEFAULT_TIMEOUT_MS

    /** Timestamp when the app last went to background. -1 = not backgrounded. */
    private var backgroundedAtMs: Long = -1L

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Called by [BugListNavHost] immediately after successful biometric authentication.
     * Marks the session as active.
     */
    fun onAuthenticated() {
        _isAuthenticated.value = true
        backgroundedAtMs = -1L
    }

    /**
     * Explicitly locks the session (e.g. user taps "Lock Now" in Settings).
     * Clears authentication state.
     */
    fun lock() {
        _isAuthenticated.value = false
    }

    // ── ProcessLifecycleOwner callbacks ──────────────────────────────────────

    /**
     * App moved to background — record the timestamp.
     * Called when ALL activities are stopped (true background).
     */
    override fun onStop(owner: LifecycleOwner) {
        if (_isAuthenticated.value) {
            backgroundedAtMs = System.currentTimeMillis()
        }
    }

    /**
     * App returned to foreground — check if the timeout has elapsed.
     * If yes: lock. If no: continue the session.
     */
    override fun onStart(owner: LifecycleOwner) {
        val bgAt = backgroundedAtMs
        if (bgAt == -1L) return
        backgroundedAtMs = -1L

        if (!_isAuthenticated.value) return

        val elapsed = System.currentTimeMillis() - bgAt
        if (autoLockTimeoutMs == 0L || elapsed >= autoLockTimeoutMs) {
            lock()
        }
    }
}
