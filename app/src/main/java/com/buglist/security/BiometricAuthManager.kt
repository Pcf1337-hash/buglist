package com.buglist.security

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
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
    /** Authentication succeeded. The [cipher] is ready for cryptographic operations. */
    data class Success(val cipher: javax.crypto.Cipher) : AuthResult()

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
        if (!isBiometricAvailable()) {
            onResult(AuthResult.HardwareUnavailable(checkBiometricAvailability()))
            return
        }

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

        // CRITICAL: Always authenticate with CryptoObject. Never call
        // biometricPrompt.authenticate(promptInfo) without CryptoObject.
        // See L-024 in lessons.md.
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
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
        if (!isBiometricAvailable()) {
            onResult(AuthResult.HardwareUnavailable(checkBiometricAvailability()))
            return
        }

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
