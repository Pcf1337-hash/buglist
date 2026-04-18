package com.buglist.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.security.AuthResult
import com.buglist.security.BiometricAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.inject.Inject

/**
 * UI state for the authentication screen.
 */
sealed class AuthUiState {
    /** Initial state — prompt not yet shown. */
    object Idle : AuthUiState()

    /** BiometricPrompt is currently showing. */
    object Authenticating : AuthUiState()

    /**
     * Authentication succeeded.
     * [cipher] is non-null on BIOMETRIC_STRONG path, null on fallback path
     * (Samsung Galaxy A series / BIOMETRIC_WEAK / DEVICE_CREDENTIAL).
     */
    data class Authenticated(val cipher: Cipher? = null) : AuthUiState()

    /** Authentication failed with an error message. [retryCount] tracks attempts. */
    data class Error(val message: String, val errorCode: Int = -1, val retryCount: Int = 0) : AuthUiState()

    /** The Keystore key was permanently invalidated — user must re-enroll. */
    object KeyInvalidated : AuthUiState()

    /** Max retries exceeded — prompt locked out. */
    object LockedOut : AuthUiState()
}

/**
 * ViewModel for the authentication screen.
 *
 * Exposes [uiState] as a [StateFlow] — never mutableStateOf (see L-011 in lessons.md).
 * The BiometricPrompt itself is launched from the Screen via a [LaunchedEffect]
 * that observes [shouldShowPrompt], because BiometricPrompt requires a FragmentActivity
 * reference which cannot be held in a ViewModel.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Emits true when the screen should trigger the BiometricPrompt. */
    private val _shouldShowPrompt = MutableStateFlow(false)
    val shouldShowPrompt: StateFlow<Boolean> = _shouldShowPrompt.asStateFlow()

    private var retryCount = 0

    /** Returns true if biometric auth hardware is available. */
    fun isBiometricAvailable(): Boolean = biometricAuthManager.isBiometricAvailable()

    /** Returns true if at least device credential is available as fallback. */
    fun isDeviceCredentialAvailable(): Boolean = biometricAuthManager.isDeviceCredentialAvailable()

    /**
     * Called by the screen when it first appears or the user taps "Retry".
     * Sets the state to [AuthUiState.Authenticating] and signals the screen
     * to show the BiometricPrompt.
     */
    fun requestAuthentication() {
        // Idempotency guard: don't re-trigger if a prompt is already in flight.
        // Both LaunchedEffect(Unit) + LifecycleResumeEffect can fire close together on
        // cold start — without this guard the second call would cancel the first prompt.
        if (_uiState.value is AuthUiState.Authenticating) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Authenticating
            _shouldShowPrompt.value = true
        }
    }

    /** Called by the screen after it has triggered the BiometricPrompt. */
    fun onPromptShown() {
        _shouldShowPrompt.value = false
    }

    /**
     * Called by the screen with the result from [BiometricAuthManager.authenticate].
     */
    fun onAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                retryCount = 0
                _uiState.value = AuthUiState.Authenticated(result.cipher)
            }

            AuthResult.SuccessNoCipher -> {
                // Fallback path: Samsung Galaxy A series / BIOMETRIC_WEAK / DEVICE_CREDENTIAL.
                // Cipher is null — biometrics used as gate only. PassphraseManager (Tink)
                // handles DB passphrase independently. See L-088 in lessons.md.
                retryCount = 0
                _uiState.value = AuthUiState.Authenticated(cipher = null)
            }

            is AuthResult.Failure -> {
                // Only count genuine hardware/auth errors toward lockout.
                // System-initiated cancels (app backgrounded → ERROR_CANCELED=5,
                // ERROR_USER_CANCELED=10) and user-tapped cancel (ERROR_NEGATIVE_BUTTON=13)
                // must NOT increment the counter — the OS handles biometric lockout itself.
                val isSystemOrUserCancel = result.errorCode in setOf(
                    androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON,  // 13 – user tapped cancel
                    androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED,    // 10 – home/back pressed
                    androidx.biometric.BiometricPrompt.ERROR_CANCELED          //  5 – hardware/system cancel
                )
                if (!isSystemOrUserCancel) retryCount++
                if (retryCount >= MAX_RETRY_ATTEMPTS) {
                    _uiState.value = AuthUiState.LockedOut
                } else {
                    _uiState.value = AuthUiState.Error(
                        message = result.message,
                        errorCode = result.errorCode,
                        retryCount = retryCount
                    )
                }
            }

            is AuthResult.HardwareUnavailable -> {
                _uiState.value = AuthUiState.Error(
                    message = "Biometric hardware unavailable: ${result.reason}"
                )
            }

            AuthResult.KeyInvalidated -> {
                _uiState.value = AuthUiState.KeyInvalidated
            }
        }
    }

    /**
     * Reset state for retry — called when the user taps the retry button, the fingerprint
     * icon, or the "ANTIPPEN ZUM ENTSPERREN" fallback CTA.
     *
     * The app-level [AuthUiState.LockedOut] is intentionally NOT a hard barrier here:
     * Android's biometric hardware already enforces its own lockout independently. Our
     * counter only guards against rapid-fire soft errors (e.g. bad hardware config). Once
     * the user explicitly requests a retry, we honour that request and reset the counter.
     */
    fun resetForRetry() {
        retryCount = 0  // reset so LockedOut doesn't immediately re-trigger
        requestAuthentication()
    }

    /**
     * Called by [LifecycleResumeEffect.onPauseOrDispose] when the screen is paused or leaves
     * composition. Resets the prompt signal so the next [requestAuthentication] call on resume
     * triggers a fresh BiometricPrompt.
     *
     * Also resets [retryCount] so that accumulated system-cancel errors (from previous
     * backgrounding events) never carry over into the next session and cause a premature
     * [AuthUiState.LockedOut] on what is effectively a fresh authentication attempt.
     */
    fun cancelAuthentication() {
        _shouldShowPrompt.value = false
        retryCount = 0  // always reset – system cancels must not accumulate across sessions
        // Only reset to Idle if not already authenticated — avoids flickering if auth succeeded
        // just before the pause (e.g., app put to background immediately after unlock).
        if (_uiState.value !is AuthUiState.Authenticated) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
