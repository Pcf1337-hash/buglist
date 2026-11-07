package com.buglist.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dumb mailbox. Each message is one encrypted snapshot. The title is a dot,
 * so a notification cannot show who owes what. The app keeps no message log.
 */
@Singleton
class NtfyMailbox @Inject constructor() {
    private val http = HttpClient(Android) {
        engine {
            connectTimeout = 12_000
            socketTimeout = 12_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun publish(topic: String, payload: String) {
        http.post("https://ntfy.sh/$topic") {
            header("Title", ".")
            header("Priority", "1")
            header("Tags", "")
            setBody(payload)
        }
    }

    suspend fun latestPayload(topic: String): String? {
        val body = http.get("https://ntfy.sh/$topic/json?poll=1&since=7d").bodyAsText()
        var newest: MailEvent? = null
        for (line in body.lineSequence()) {
            if (line.isBlank()) continue
            val event = try {
                json.decodeFromString<MailEvent>(line)
            } catch (_: Exception) {
                continue
            }
            if (event.event != "message" || event.message.isNullOrBlank()) continue
            if (newest == null || event.time > newest.time) newest = event
        }
        return newest?.message
    }

    @Serializable
    private data class MailEvent(
        val event: String = "",
        val time: Long = 0,
        val message: String? = null
    )
}
