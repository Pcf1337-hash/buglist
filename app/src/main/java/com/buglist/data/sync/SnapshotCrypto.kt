package com.buglist.data.sync

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts one full book snapshot. The mailbox stores this blob and nothing else:
 * no names in headers, no per-change log.
 */
object SnapshotCrypto {
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128

    fun encrypt(plaintext: ByteArray, keyBase64Url: String): String {
        val key = decodeKey(keyBase64Url)
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            val body = cipher.doFinal(plaintext)
            val packed = ByteArray(iv.size + body.size)
            iv.copyInto(packed)
            body.copyInto(packed, iv.size)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(packed)
        } finally {
            key.fill(0)
        }
    }

    fun decrypt(payload: String, keyBase64Url: String): ByteArray? {
        val packed = try {
            Base64.getUrlDecoder().decode(payload)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (packed.size <= IV_SIZE) return null
        val key = decodeKey(keyBase64Url)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(TAG_BITS, packed.copyOfRange(0, IV_SIZE))
            )
            cipher.doFinal(packed, IV_SIZE, packed.size - IV_SIZE)
        } catch (_: Exception) {
            null
        } finally {
            key.fill(0)
        }
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun decodeKey(keyBase64Url: String): ByteArray =
        Base64.getUrlDecoder().decode(keyBase64Url)
}
