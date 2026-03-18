package com.buglist.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties.AUTH_BIOMETRIC_STRONG
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import android.security.keystore.KeyProperties.KEY_ALGORITHM_AES
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted data container holding the IV and ciphertext produced by [KeystoreManager].
 *
 * @property iv          The GCM initialization vector (12 bytes).
 * @property ciphertext  The AES-GCM encrypted payload.
 */
data class EncryptedData(
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedData
        return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int = 31 * iv.contentHashCode() + ciphertext.contentHashCode()
}

/**
 * Manages the AES-256-GCM key stored in the Android Keystore.
 *
 * Security properties enforced:
 * - Key requires BIOMETRIC_STRONG authentication before use.
 * - Key is invalidated when new biometrics are enrolled ([setInvalidatedByBiometricEnrollment]).
 * - StrongBox (Titan/SE chip) is preferred; falls back to TEE if unavailable.
 *   StrongBox is NOT used for bulk encryption (55x slower) — only for key generation.
 * - GCM tag length: 128 bits. IV: 12 bytes (randomly generated per encryption).
 *
 * @param context Application context used for StrongBox feature detection.
 */
@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val KEY_ALIAS = "buglist_db_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val KEY_SIZE_BITS = 256
    }

    /**
     * Returns a [Cipher] initialized for encryption, backed by the Keystore key.
     *
     * This cipher must be wrapped in a [BiometricPrompt.CryptoObject] and presented
     * to the user for biometric authentication before [encrypt] can be called.
     *
     * @throws KeyPermanentlyInvalidatedException if biometric enrollment changed.
     *         Callers must catch this, call [deleteKey], regenerate, and re-auth.
     */
    fun getEncryptCipher(): Cipher {
        val key = getOrCreateKey()
        return Cipher.getInstance(AES_GCM_NO_PADDING).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    /**
     * Returns a [Cipher] initialized for decryption using the provided [iv].
     *
     * @param iv The initialization vector saved from a previous [encrypt] call.
     * @throws KeyPermanentlyInvalidatedException if biometric enrollment changed.
     */
    fun getDecryptCipher(iv: ByteArray): Cipher {
        val key = getOrCreateKey()
        return Cipher.getInstance(AES_GCM_NO_PADDING).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
    }

    /**
     * Encrypts [plaintext] using an already-authenticated [cipher] (from [getEncryptCipher]).
     *
     * @param plaintext The data to encrypt. Caller is responsible for zeroing this after use.
     * @param cipher    An authenticated cipher from a successful [BiometricPrompt] callback.
     * @return [EncryptedData] containing the IV and ciphertext.
     */
    fun encrypt(plaintext: ByteArray, cipher: Cipher): EncryptedData {
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedData(
            iv = cipher.iv,
            ciphertext = ciphertext
        )
    }

    /**
     * Decrypts [encryptedData] using an already-authenticated [cipher] (from [getDecryptCipher]).
     *
     * @param encryptedData The [EncryptedData] produced by a previous [encrypt] call.
     * @param cipher        An authenticated cipher from a successful [BiometricPrompt] callback.
     * @return The decrypted plaintext as [ByteArray]. Caller must zero this after use.
     */
    fun decrypt(encryptedData: EncryptedData, cipher: Cipher): ByteArray {
        return cipher.doFinal(encryptedData.ciphertext)
    }

    /**
     * Returns true if the Keystore key currently exists.
     */
    fun keyExists(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: KeyStoreException) {
            false
        }
    }

    /**
     * Deletes the Keystore key. Must be called when [KeyPermanentlyInvalidatedException]
     * is caught so a new key can be generated on the next [getOrCreateKey] call.
     */
    fun deleteKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: KeyStoreException) {
            // Key may already be gone; safe to ignore
        }
    }

    /**
     * Retrieves the existing key from the Keystore, or generates a new one if absent.
     *
     * Key generation attempts StrongBox first (hardware security module / Titan chip).
     * If the device does not have StrongBox, falls back to TEE-backed key generation.
     *
     * Note: StrongBox is only used for KEY GENERATION — not for bulk encryption.
     * The actual AES-GCM cipher operations run in the TEE-backed software layer,
     * which is ~55x faster than running inside StrongBox. See L-023 in lessons.md.
     *
     * @throws KeyPermanentlyInvalidatedException if the key was invalidated by a
     *         biometric enrollment change. Callers must handle this.
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }

        return generateKey()
    }

    /**
     * Generates a new AES-256-GCM key in the Android Keystore.
     * Tries StrongBox first (API 28+); falls back to standard TEE if unavailable.
     */
    private fun generateKey(): SecretKey {
        // FEATURE_STRONGBOX_KEYSTORE and setIsStrongBoxBacked require API 28.
        val hasStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

        return if (hasStrongBox) {
            try {
                generateKeyWithSpec(useStrongBox = true)
            } catch (e: Exception) {
                // StrongBox may fail on some devices even if advertised
                generateKeyWithSpec(useStrongBox = false)
            }
        } else {
            generateKeyWithSpec(useStrongBox = false)
        }
    }

    /**
     * Builds the key spec and generates the key.
     *
     * [setUserAuthenticationParameters] requires API 30 and replaces the deprecated
     * [setUserAuthenticationValidityDurationSeconds]. On API 26–29 we use the legacy
     * method — both produce BIOMETRIC_STRONG binding functionally.
     * [setIsStrongBoxBacked] requires API 28 — only passed true when already guarded above.
     */
    @SuppressLint("NewApi") // Guarded by Build.VERSION checks in generateKey()
    private fun generateKeyWithSpec(useStrongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
            .setBlockModes(BLOCK_MODE_GCM)
            .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)  // Mandatory security requirement

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: modern, explicit biometric-strong binding
            builder.setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)
        } else {
            // API 26–29: timeout=0 means "require auth for every use" (same semantics)
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        val spec = builder.build()
        return KeyGenerator.getInstance(KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(spec)
        }.generateKey()
    }
}
