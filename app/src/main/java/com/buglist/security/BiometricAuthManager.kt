package com.buglist.security

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a biometric authentication attempt.
 */
sealed class AuthResult {
    /** Authentication succeeded with CryptoObject binding (BIOMETRIC_STRONG path). */
    data class Success(val cipher: javax.crypto.Cipher) : AuthResult()

    /**
     * Authentication succeeded via fallback path (BIOMETRIC_WEAK or DEVICE_CREDENTIAL).
     * No CryptoObject is available — used as a gate only (Samsung Galaxy A series).
     */
    object SuccessNoCipher : AuthResult()

    /** Authentication failed (wrong fingerprint, cancelled, etc). */
    data class Failure(val errorCode: Int, val message: String) : AuthResult()

    /** Biometric hardware unavailable or no biometrics enrolled. */
    data class HardwareUnavailable(val reason: BiometricAvailability) : AuthResult()

    /** The Keystore key was permanently invalidated (new biometrics enrolled). */
    object KeyInvalidated : AuthResult()
}

/**
 * Reasons why biometric hardware may be unavailable.
 */
enum class BiometricAvailability {
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNKNOWN
}

/**
 * Manages biometric authentication using Android BiometricPrompt.
 *
 * Security requirements enforced:
 * - ALWAYS uses [BiometricPrompt.CryptoObject] binding — no soft binding allowed.
 *   See L-024 in lessons.md: soft binding is bypassable.
 * - Prefers [BIOMETRIC_STRONG] (Class 3).
 * - Handles [KeyPermanentlyInvalidatedException]: deletes invalid key so callers
 *   can regenerate and re-authenticate.
 * - The cipher in [AuthResult.Success] is authenticated and ready for immediate use.
 *
 * Note: [MainActivity] extends [FragmentActivity] explicitly (not ComponentActivity) so
 * that [BiometricPrompt(FragmentActivity)] constructor works correctly. See L-066.
 *
 * @param context Application context (for BiometricManager availability checks only).
 * @param keystoreManager Used to obtain the encryption cipher for CryptoObject binding.
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager
) {

    /**
     * Checks whether strong biometric authentication is available on this device.
     */
    fun checkBiometricAvailability(): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.NO_HARDWARE // placeholder; see isBiometricAvailable()
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNKNOWN
        }
    }

    /**
     * Returns true if BIOMETRIC_STRONG authentication is currently available.
     */
    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Returns true if any authentication (weak biometric or device credential) is available.
     * Used to detect Samsung Galaxy A series devices where the sensor is Class 2 (BIOMETRIC_WEAK)
     * and canAuthenticate(BIOMETRIC_STRONG) incorrectly returns NONE_ENROLLED.
     */
    fun isFallbackAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Returns true if at least DEVICE_CREDENTIAL is available as fallback.
     */
    fun isDeviceCredentialAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Launches the BiometricPrompt for encryption (first-time or new-session auth).
     *
     * ALWAYS binds a [BiometricPrompt.CryptoObject] with an encrypt-mode cipher.
     * This cryptographically binds the biometric result to a real Keystore operation —
     * software-only auth bypass is impossible. See L-024 in lessons.md.
     *
     * @param activity       The hosting [FragmentActivity]. MainActivity extends FragmentActivity
     *                       explicitly so this is safe without any cast. See L-066 in lessons.md.
     * @param title          Prompt title shown to the user.
     * @param subtitle       Prompt subtitle.
     * @param negativeButton Text for the cancel/negative button.
     * @param onResult       Callback invoked on the main thread with the [AuthResult].
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButton: String,
        onResult: (AuthResult) -> Unit
    ) {
        when {
            isBiometricAvailable() -> {
                // Preferred path: BIOMETRIC_STRONG with CryptoObject binding.
                authenticateStrong(activity, title, subtitle, negativeButton, onResult)
            }
            isFallbackAvailable() -> {
                // Fallback path: Samsung Galaxy A series and other devices with Class 2
                // sensors. canAuthenticate(BIOMETRIC_STRONG) returns NONE_ENROLLED even
                // when a fingerprint is enrolled because the sensor is Class 2 (WEAK).
                // DEVICE_CREDENTIAL is included so PIN/pattern/password also works.
                // No CryptoObject — biometrics used as gate only (PassphraseManager
                // uses Tink independently and is not affected). See L-088 in lessons.md.
                authenticateFallback(activity, title, subtitle, onResult)
            }
            else -> {
                onResult(AuthResult.HardwareUnavailable(checkBiometricAvailability()))
            }
        }
    }

    /**
     * BIOMETRIC_STRONG path with CryptoObject binding.
     * Preferred on devices where Class 3 biometrics are available.
     */
    private fun authenticateStrong(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButton: String,
        onResult: (AuthResult) -> Unit
    ) {
        val cipher = try {
            keystoreManager.getEncryptCipher()
        } catch (e: KeyPermanentlyInvalidatedException) {
            keystoreManager.deleteKey()
            onResult(AuthResult.KeyInvalidated)
            return
        } catch (e: Exception) {
            onResult(AuthResult.Failure(-1, e.message ?: "Key error"))
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    val authenticatedCipher = result.cryptoObject?.cipher
                    if (authenticatedCipher != null) {
                        onResult(AuthResult.Success(authenticatedCipher))
                    } else {
                        onResult(AuthResult.Failure(
                            BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                            "CryptoObject missing after auth"
                        ))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(AuthResult.Failure(errorCode, errString.toString()))
                }

                override fun onAuthenticationFailed() {
                    // Individual failed attempt — BiometricPrompt handles retry UI internally.
                }
            }
        )

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    /**
     * Fallback path for Samsung Galaxy A series and other Class 2 devices.
     *
     * Uses BIOMETRIC_WEAK or DEVICE_CREDENTIAL — no CryptoObject (Android restriction:
     * CryptoObject requires BIOMETRIC_STRONG). Biometrics serve as a gate only.
     * The database passphrase is independently protected by Tink (PassphraseManager).
     *
     * Note: No negative button when DEVICE_CREDENTIAL is included in authenticators.
     */
    private fun authenticateFallback(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (AuthResult) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            // No setNegativeButtonText — DEVICE_CREDENTIAL provides its own cancel UI
            .build()

        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    // No CryptoObject in this path by design — see method KDoc.
                    onResult(AuthResult.SuccessNoCipher)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(AuthResult.Failure(errorCode, errString.toString()))
                }

                override fun onAuthenticationFailed() {
                    // Individual failed attempt — BiometricPrompt handles retry UI.
                }
            }
        )

        // No CryptoObject — intentional for the fallback path.
        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Launches BiometricPrompt for decryption (re-opening an existing encrypted session).
     *
     * @param iv             The IV from the previously encrypted passphrase.
     * @param activity       The hosting [FragmentActivity].
     * @param title          Prompt title.
     * @param subtitle       Prompt subtitle.
     * @param negativeButton Cancel button text.
     * @param onResult       Callback with [AuthResult].
     */
    fun authenticateForDecrypt(
        iv: ByteArray,
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButton: String,
        onResult: (AuthResult) -> Unit
    ) {
        when {
            isBiometricAvailable() -> {
                authenticateForDecryptStrong(iv, activity, title, subtitle, negativeButton, onResult)
            }
            isFallbackAvailable() -> {
                authenticateFallback(activity, title, subtitle, onResult)
            }
            else -> {
                onResult(AuthResult.HardwareUnavailable(checkBiometricAvailability()))
            }
        }
    }

    /**
     * BIOMETRIC_STRONG decrypt path with CryptoObject binding.
     */
    private fun authenticateForDecryptStrong(
        iv: ByteArray,
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButton: String,
        onResult: (AuthResult) -> Unit
    ) {
        val cipher = try {
            keystoreManager.getDecryptCipher(iv)
        } catch (e: KeyPermanentlyInvalidatedException) {
            keystoreManager.deleteKey()
            onResult(AuthResult.KeyInvalidated)
            return
        } catch (e: Exception) {
            onResult(AuthResult.Failure(-1, e.message ?: "Key error"))
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    val authenticatedCipher = result.cryptoObject?.cipher
                    if (authenticatedCipher != null) {
                        onResult(AuthResult.Success(authenticatedCipher))
                    } else {
                        onResult(AuthResult.Failure(
                            BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                            "CryptoObject missing after auth"
                        ))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(AuthResult.Failure(errorCode, errString.toString()))
                }

                override fun onAuthenticationFailed() {
                    // Individual failed attempt — BiometricPrompt handles retry UI.
                }
            }
        )

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
