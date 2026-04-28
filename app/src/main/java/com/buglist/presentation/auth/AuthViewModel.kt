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
 *
 * Since the app is fully closed when the user navigates away ([MainActivity.onUserLeaveHint]),
 * there is no background-return scenario. The auth screen is always a fresh cold start.
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
        // Idempotency guard: don't re-trigger if a prompt is already in flight or
        // auth already succeeded. Both LaunchedEffect(Unit) and the OnWindowFocusChangeListener
        // can fire close together — without this guard multiple concurrent calls
        // could cancel each other.
        val current = _uiState.value
        if (current is AuthUiState.Authenticating || current is AuthUiState.Authenticated) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Authenticating
            _shouldShowPrompt.value = true

            // Safety net: some OEM ROMs silently drop BiometricPrompt.authenticate()
            // without ever calling onAuthenticationError. If we're still Authenticating
            // after 6 s with no callback, reset to Idle so the "TAP TO UNLOCK" fallback
            // becomes visible.
            kotlinx.coroutines.delay(6_000L)
            if (_uiState.value is AuthUiState.Authenticating) {
                _uiState.value = AuthUiState.Idle
            }
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
                // System-initiated cancels (ERROR_CANCELED=5, ERROR_USER_CANCELED=10)
                // and user-tapped cancel (ERROR_NEGATIVE_BUTTON=13) must NOT increment
                // the counter — the OS handles biometric lockout itself.
                val isSystemOrUserCancel = result.errorCode in setOf(
                    androidx.biometric.BiometricPrompt.ERROR_CANCELED,          // 5
                    androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED,     // 10
                    androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON    // 13
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
     * Reset state for retry — called when the user taps the retry button or the
     * "ANTIPPEN ZUM ENTSPERREN" fallback CTA.
     */
    fun resetForRetry() {
        retryCount = 0
        requestAuthentication()
    }

    /**
     * Called by [LifecycleResumeEffect.onPauseOrDispose] when the screen is paused.
     * Resets the prompt signal and retry counter so state never carries over.
     */
    fun cancelAuthentication() {
        _shouldShowPrompt.value = false
        retryCount = 0
        if (_uiState.value !is AuthUiState.Authenticated) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
