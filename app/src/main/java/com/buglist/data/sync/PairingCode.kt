package com.buglist.data.sync

import java.security.SecureRandom
import java.util.Base64

/**
 * In-person pairing payload. The key travels only inside this code (QR or paste).
 * The mailbox never sees it.
 *
 * Shape: `BL1.<room-hex>.<key-base64url>.<H|B>`
 * H = scanner is a guest (this phone leads). B = both may edit.
 */
data class PairingOffer(
    val roomId: String,
    val keyBase64Url: String,
    val scannerIsGuest: Boolean
)

object PairingCode {
    private const val PREFIX = "BL1"

    fun randomRoomId(): String = randomBytes(16).toHex()

    fun randomKey(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32))

    fun randomDeviceId(): String = randomBytes(8).toHex()

    fun encode(roomId: String, keyBase64Url: String, bothCanEdit: Boolean): String {
        val flag = if (bothCanEdit) "B" else "H"
        return "$PREFIX.$roomId.$keyBase64Url.$flag"
    }

    fun decode(raw: String): PairingOffer? {
        val parts = raw.trim().split('.')
        if (parts.size != 4 || parts[0] != PREFIX) return null
        val room = parts[1]
        val key = parts[2]
        val flag = parts[3]
        if (room.length != 32 || room.any { it !in HEX }) return null
        if (flag != "H" && flag != "B") return null
        // Reject malformed pasted keys before storing a pairing or encrypting.
        // AES-256 requires exactly 32 decoded bytes, not 32 text characters.
        val keyBytes = runCatching { Base64.getUrlDecoder().decode(key) }.getOrNull() ?: return null
        val validKey = keyBytes.size == 32
        keyBytes.fill(0)
        if (!validKey) return null
        return PairingOffer(
            roomId = room,
            keyBase64Url = key,
            scannerIsGuest = flag == "H"
        )
    }

    fun topicFor(roomId: String): String = "bl$roomId"

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { SecureRandom().nextBytes(it) }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private val HEX = ('0'..'9') + ('a'..'f')
