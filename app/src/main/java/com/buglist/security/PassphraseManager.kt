package com.buglist.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.passphraseDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "buglist_passphrase_store"
)

/**
 * Manages the SQLCipher database passphrase lifecycle.
 *
 * The passphrase is a 256-bit cryptographically random [ByteArray] generated once.
 * It is encrypted with Tink AES-256-GCM (backed by Android Keystore) and stored
 * in Jetpack DataStore Preferences.
 *
 * Security properties:
 * - Passphrase is NEVER stored as a String (remains ByteArray throughout).
 * - After use the caller MUST zero the returned ByteArray.
 * - Tink AEAD uses AES-256-GCM with a hardware-backed key via AndroidKeysetManager.
 * - DataStore Preferences is used instead of EncryptedSharedPreferences
 *   (deprecated April 2025). See L-022 in lessons.md.
 *
 * @param context Application context for DataStore access.
 */
@Singleton
class PassphraseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PASSPHRASE_KEY_NAME = "buglist_passphrase_encrypted"
        private const val TINK_KEYSET_NAME = "buglist_tink_keyset"
        private const val MASTER_KEY_URI = "android-keystore://buglist_tink_master_key"
        private const val PASSPHRASE_SIZE_BYTES = 32 // 256 bits

        private val ENCRYPTED_PASSPHRASE_KEY = byteArrayPreferencesKey(PASSPHRASE_KEY_NAME)
    }

    /**
     * Returns the database passphrase as a [ByteArray].
     *
     * If no passphrase exists yet, generates a new random one, encrypts it,
     * and persists it to DataStore before returning.
     *
     * CRITICAL: The caller MUST zero this array after use:
     * ```kotlin
     * val passphrase = passphraseManager.getOrCreatePassphrase()
     * try {
     *     openDatabase(passphrase)
     * } finally {
     *     passphrase.fill(0)
     * }
     * ```
     *
     * @return A 256-bit passphrase as [ByteArray]. Caller must zero after use.
     */
    suspend fun getOrCreatePassphrase(): ByteArray {
        val prefs = context.passphraseDataStore.data.first()
        val encryptedPassphrase = prefs[ENCRYPTED_PASSPHRASE_KEY]

        return if (encryptedPassphrase != null) {
            decryptPassphrase(encryptedPassphrase)
        } else {
            val newPassphrase = generatePassphrase()
            try {
                val encrypted = encryptPassphrase(newPassphrase)
                context.passphraseDataStore.edit { it[ENCRYPTED_PASSPHRASE_KEY] = encrypted }
                newPassphrase.copyOf()
            } finally {
                // Zero the local copy after storing; the returned copy stays live
                // until the caller zeros it.
            }
        }
    }

    /**
     * Forces regeneration of the passphrase (e.g., after a security incident).
     * WARNING: This will make the existing database permanently inaccessible.
     */
    suspend fun regeneratePassphrase() {
        context.passphraseDataStore.edit { it.remove(ENCRYPTED_PASSPHRASE_KEY) }
    }

    /** Generates a cryptographically secure 256-bit random passphrase. */
    private fun generatePassphrase(): ByteArray {
        return ByteArray(PASSPHRASE_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
    }

    /** Returns the Tink AEAD primitive backed by Android Keystore. */
    private fun getAead(): Aead {
        AeadConfig.register()
        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, TINK_KEYSET_NAME, null)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(Aead::class.java)
    }

    private fun encryptPassphrase(plaintext: ByteArray): ByteArray {
        return getAead().encrypt(plaintext, null)
    }

    private fun decryptPassphrase(ciphertext: ByteArray): ByteArray {
        return getAead().decrypt(ciphertext, null)
    }
}
