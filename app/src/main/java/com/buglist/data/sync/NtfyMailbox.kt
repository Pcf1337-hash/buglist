package com.buglist.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
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
        expectSuccess = true
        engine {
            connectTimeout = 12_000
            socketTimeout = 12_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun publish(topic: String, payload: String) {
        retry {
            http.post("https://ntfy.sh/$topic") {
                header("Title", ".")
                header("Priority", "1")
                setBody(payload)
            }
        }
    }

    /** Cached mailbox messages, oldest first. ntfy keeps them for about twelve hours. */
    suspend fun messages(topic: String): List<String> = retry {
        val body = http.get("https://ntfy.sh/$topic/json?poll=1&since=all").bodyAsText()
        body.lineSequence().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val event = try {
                json.decodeFromString<MailEvent>(line)
            } catch (_: Exception) {
                return@mapNotNull null
            }
            if (event.event != "message") null else event.message?.takeIf { it.isNotBlank() }
        }.toList()
    }

    private suspend fun <T> retry(block: suspend () -> T): T {
        var last: Exception? = null
        repeat(3) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                last = e
                if (attempt < 2) delay(400L * (attempt + 1))
            }
        }
        throw last ?: IllegalStateException("mailbox")
    }

    @Serializable
    private data class MailEvent(
        val event: String = "",
        val time: Long = 0,
        val message: String? = null
    )
}
