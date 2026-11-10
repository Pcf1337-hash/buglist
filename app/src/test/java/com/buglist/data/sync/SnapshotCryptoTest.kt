package com.buglist.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotCryptoTest {

    @Test
    fun roundTripHidesThePlaintext() {
        val key = PairingCode.randomKey()
        val code = PairingCode.encode(PairingCode.randomRoomId(), key, bothCanEdit = true)
        val offer = PairingCode.decode(code)!!
        assertEquals(false, offer.scannerIsGuest)
        val plain = """{"from":"abc","debts":[]}""".toByteArray()
        val blob = SnapshotCrypto.encrypt(plain, offer.keyBase64Url)
        assertNotEquals(String(plain), blob)
        val back = SnapshotCrypto.decrypt(blob, offer.keyBase64Url)
        assertEquals(String(plain), back?.decodeToString())
    }

    @Test
    fun wrongKeyDoesNotOpenTheBook() {
        val blob = SnapshotCrypto.encrypt("geheim".toByteArray(), PairingCode.randomKey())
        assertNull(SnapshotCrypto.decrypt(blob, PairingCode.randomKey()))
    }

    @Test
    fun hostCodeMakesTheScannerAGuest() {
        val code = PairingCode.encode(PairingCode.randomRoomId(), PairingCode.randomKey(), bothCanEdit = false)
        assertEquals(true, PairingCode.decode(code)!!.scannerIsGuest)
        assertNull(PairingCode.decode("keine-kopplung"))
    }

    @Test
    fun malformedKeysAreRejectedBeforePairing() {
        val room = PairingCode.randomRoomId()
        for (key in listOf("!".repeat(43), "A".repeat(32), "A".repeat(64))) {
            assertNull(PairingCode.decode("BL1.$room.$key.H"))
        }
    }
}
