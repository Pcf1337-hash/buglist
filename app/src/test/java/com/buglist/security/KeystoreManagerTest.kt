package com.buglist.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher

/**
 * Unit tests for [KeystoreManager].
 *
 * Note: Android Keystore hardware operations cannot run in JVM unit tests —
 * they require a real device or emulator. These tests focus on:
 * - [EncryptedData] data class contract (equals/hashCode/copy)
 * - encrypt/decrypt round-trip using a mock Cipher
 * - Logic paths (StrongBox fallback, key existence check) are tested
 *   in instrumented tests in the androidTest source set.
 */
class KeystoreManagerTest {

    // ── EncryptedData contract ──────────────────────────────────────────────

    @Test
    fun `EncryptedData equals when same content`() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val ciphertext = byteArrayOf(0x10, 0x20, 0x30)
        val a = EncryptedData(iv, ciphertext)
        val b = EncryptedData(iv.copyOf(), ciphertext.copyOf())

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `EncryptedData not equals when iv differs`() {
        val ciphertext = byteArrayOf(0x10, 0x20)
        val a = EncryptedData(byteArrayOf(1, 2, 3), ciphertext)
        val b = EncryptedData(byteArrayOf(4, 5, 6), ciphertext)

        assertFalse(a == b)
    }

    @Test
    fun `EncryptedData not equals when ciphertext differs`() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val a = EncryptedData(iv, byteArrayOf(0x10))
        val b = EncryptedData(iv.copyOf(), byteArrayOf(0x20))

        assertFalse(a == b)
    }

    @Test
    fun `EncryptedData iv and ciphertext are accessible`() {
        val iv = byteArrayOf(1, 2, 3)
        val ciphertext = byteArrayOf(4, 5, 6)
        val data = EncryptedData(iv, ciphertext)

        assertArrayEquals(iv, data.iv)
        assertArrayEquals(ciphertext, data.ciphertext)
    }

    // ── encrypt/decrypt with mock Cipher ───────────────────────────────────

    @Test
    fun `encrypt uses cipher doFinal and returns EncryptedData with correct iv`() {
        val plaintext = "test-secret".toByteArray()
        val fakeCiphertext = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        val fakeIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)

        val cipher = mockk<Cipher>()
        every { cipher.doFinal(plaintext) } returns fakeCiphertext
        every { cipher.iv } returns fakeIv

        // KeystoreManager.encrypt() only needs context for key generation —
        // we mock the cipher directly to avoid Keystore hardware dependency.
        val context = mockk<android.content.Context>(relaxed = true)
        val manager = KeystoreManager(context)

        val result = manager.encrypt(plaintext, cipher)

        assertArrayEquals(fakeIv, result.iv)
        assertArrayEquals(fakeCiphertext, result.ciphertext)
        verify(exactly = 1) { cipher.doFinal(plaintext) }
    }

    @Test
    fun `decrypt uses cipher doFinal and returns plaintext`() {
        val expectedPlaintext = "test-secret".toByteArray()
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val ciphertext = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        val encryptedData = EncryptedData(iv, ciphertext)

        val cipher = mockk<Cipher>()
        every { cipher.doFinal(ciphertext) } returns expectedPlaintext

        val context = mockk<android.content.Context>(relaxed = true)
        val manager = KeystoreManager(context)

        val result = manager.decrypt(encryptedData, cipher)

        assertArrayEquals(expectedPlaintext, result)
        verify(exactly = 1) { cipher.doFinal(ciphertext) }
    }

    @Test
    fun `encrypt then decrypt with mock ciphers is a round trip`() {
        val plaintext = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val iv = ByteArray(12) { it.toByte() }
        val ciphertext = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())

        val encryptCipher = mockk<Cipher>()
        every { encryptCipher.doFinal(plaintext) } returns ciphertext
        every { encryptCipher.iv } returns iv

        val decryptCipher = mockk<Cipher>()
        every { decryptCipher.doFinal(ciphertext) } returns plaintext

        val context = mockk<android.content.Context>(relaxed = true)
        val manager = KeystoreManager(context)

        val encrypted = manager.encrypt(plaintext, encryptCipher)
        val decrypted = manager.decrypt(encrypted, decryptCipher)

        assertArrayEquals(plaintext, decrypted)
        assertNotNull(encrypted)
        assertEquals(12, encrypted.iv.size)
    }
}
