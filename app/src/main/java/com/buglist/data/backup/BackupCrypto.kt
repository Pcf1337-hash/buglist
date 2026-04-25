package com.buglist.data.backup

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides encrypt/decrypt for BugList backup files.
 *
 * ## Binary file format
 * ```
 * [14 bytes]  Magic: "BUGLIST_BAK_V1" (ASCII)
 * [16 bytes]  Argon2 salt (random, stored in file)
 * [12 bytes]  AES-GCM IV (random, stored in file)
 * [ N bytes]  AES-256-GCM ciphertext (plaintext JSON + 16-byte GCM authentication tag)
 * ```
 *
 * ## Key derivation
 * Argon2id (t=2, m=32768 KiB, p=1) → 32-byte key
 * Salt is generated fresh per export and stored alongside ciphertext.
 *
 * ## Wrong-password detection
 * GCM authentication tag verification fails → [AEADBadTagException] is thrown by the JCA
 * layer. Callers should catch it and display "Falsches Passwort oder beschädigte Datei".
 *
 * Security notes:
 * - Password-derived ByteArray is zeroed immediately after key derivation.
 * - The caller's String password is out of our control (JVM string interning);
 *   minimizing exposure is all we can do at this layer.
 */
object BackupCrypto {

    private val MAGIC = "BUGLIST_BAK_V1".toByteArray(Charsets.US_ASCII)  // 14 bytes
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12        // 96 bits — standard for GCM
    private const val GCM_TAG_BITS = 128  // 16-byte authentication tag
    private const val KEY_BYTES = 32      // AES-256

    // Argon2id parameters — balanced for security and speed on mid-range Android hardware
    private const val ARGON2_T = 2        // 2 iterations
    private const val ARGON2_M = 32768    // 32 MiB memory
    private const val ARGON2_P = 1        // 1 thread

    /**
     * Encrypts [plaintext] (JSON bytes) with a password-derived key.
     *
     * @param plaintext Unencrypted JSON bytes.
     * @param password  User-supplied backup password (String for UI compatibility).
     * @return          Full backup file bytes (magic + salt + IV + ciphertext).
     */
    fun encrypt(plaintext: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            val ciphertext = cipher.doFinal(plaintext)

            ByteArrayOutputStream(MAGIC.size + SALT_SIZE + IV_SIZE + ciphertext.size).apply {
                write(MAGIC)
                write(salt)
                write(iv)
                write(ciphertext)
            }.toByteArray()
        } finally {
            key.fill(0)
        }
    }

    /**
     * Decrypts a backup file.
     *
     * @param fileBytes Complete `.blbak` file content.
     * @param password  User-supplied password.
     * @return          Decrypted JSON bytes.
     * @throws IllegalArgumentException if the file is not a valid BugList backup.
     * @throws AEADBadTagException      if the password is wrong or the file is corrupted.
     */
    fun decrypt(fileBytes: ByteArray, password: String): ByteArray {
        val minSize = MAGIC.size + SALT_SIZE + IV_SIZE + GCM_TAG_BITS / 8
        require(fileBytes.size >= minSize) { "File too small to be a valid BugList backup" }

        // Verify magic header
        val fileMagic = fileBytes.copyOfRange(0, MAGIC.size)
        require(fileMagic.contentEquals(MAGIC)) { "Not a BugList backup file (magic mismatch)" }

        var offset = MAGIC.size
        val salt = fileBytes.copyOfRange(offset, offset + SALT_SIZE)
        offset += SALT_SIZE
        val iv = fileBytes.copyOfRange(offset, offset + IV_SIZE)
        offset += IV_SIZE
        val ciphertext = fileBytes.copyOfRange(offset, fileBytes.size)

        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            // Throws AEADBadTagException if password is wrong — caller maps this to user message
            cipher.doFinal(ciphertext)
        } finally {
            key.fill(0)
        }
    }

    /**
     * Derives a 256-bit key from [password] + [salt] using Argon2id.
     * The returned ByteArray MUST be zeroed by the caller after use.
     */
    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        return try {
            Argon2Kt().hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passwordBytes,
                salt = salt,
                tCostInIterations = ARGON2_T,
                mCostInKibibyte = ARGON2_M,
                parallelism = ARGON2_P,
                hashLengthInBytes = KEY_BYTES
            ).rawHashAsByteArray()
        } finally {
            passwordBytes.fill(0)
        }
    }
}
