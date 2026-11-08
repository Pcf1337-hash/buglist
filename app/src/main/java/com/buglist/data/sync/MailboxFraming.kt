package com.buglist.data.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ntfy.sh keeps a normal message for about twelve hours, but a body around 4 KB
 * is stored as an attachment that expires after three hours. Frames stay small
 * enough to remain ordinary messages. The bytes inside are already ciphertext.
 */
internal object MailboxFraming {
    const val INLINE_LIMIT = 2_800
    private const val CHUNK = 2_000

    private val json = Json { ignoreUnknownKeys = true }

    fun frames(payload: String): List<String> {
        if (payload.length <= INLINE_LIMIT) return listOf(payload)
        val slices = payload.chunked(CHUNK)
        val id = java.util.UUID.randomUUID().toString().substring(0, 8)
        return slices.mapIndexed { index, slice ->
            json.encodeToString(Frame(id = id, index = index, count = slices.size, part = slice))
        }
    }

    fun assemble(bodies: List<String>): List<String> {
        val pending = LinkedHashMap<String, Array<String?>>()
        val ready = mutableListOf<String>()
        for (body in bodies) {
            val frame = runCatching { json.decodeFromString<Frame>(body) }.getOrNull()
            if (frame == null || frame.bl != 1 || frame.count <= 0 || frame.index !in 0 until frame.count) {
                if (!body.startsWith("You received a file:")) ready += body
                continue
            }
            val slots = pending.getOrPut(frame.id) { arrayOfNulls(frame.count) }
            if (slots.size != frame.count) continue
            slots[frame.index] = frame.part
            if (slots.all { it != null }) {
                ready += slots.joinToString(separator = "") { it.orEmpty() }
                pending.remove(frame.id)
            }
        }
        return ready
    }

    @Serializable
    internal data class Frame(
        val bl: Int = 1,
        val id: String,
        val index: Int,
        val count: Int,
        val part: String
    )
}
