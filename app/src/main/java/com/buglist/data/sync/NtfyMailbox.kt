package com.buglist.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dumb mailbox. Each message is one encrypted snapshot. The title is a dot,
 * so a notification cannot show who owes what. The app keeps no message log.
 */
@Singleton
class NtfyMailbox @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val http = HttpClient(Android) {
        expectSuccess = true
        engine {
            connectTimeout = 12_000
            socketTimeout = 12_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun publish(topic: String, payload: String) {
        ensureOnline()
        for (frame in MailboxFraming.frames(payload)) {
            retry {
                http.post("https://ntfy.sh/$topic") {
                    header("Title", ".")
                    header("Priority", "1")
                    setBody(frame)
                }
            }
        }
    }

    /**
     * Cached mailbox messages, oldest first. A body that ntfy turned into a file
     * is downloaded. Split frames are joined back into one ciphertext.
     */
    suspend fun messages(topic: String): List<String> = retry {
        ensureOnline()
        val body = http.get(
            "https://ntfy.sh/$topic/json?poll=1&since=all&nc=${System.nanoTime()}"
        ) {
            header("Cache-Control", "no-cache")
            header("Pragma", "no-cache")
        }.bodyAsText()
        val bodies = mutableListOf<String>()
        for (line in body.lineSequence()) {
            if (line.isBlank()) continue
            val event = try {
                json.decodeFromString<MailEvent>(line)
            } catch (_: Exception) {
                continue
            }
            if (event.event != "message") continue
            val attached = event.attachment?.url
            val text = if (!attached.isNullOrBlank() && attached.startsWith("https://ntfy.sh/")) {
                runCatching { http.get(attached).bodyAsText() }.getOrNull()
            } else {
                event.message
            }
            if (!text.isNullOrBlank()) bodies += text
        }
        MailboxFraming.assemble(bodies)
    }

    private fun ensureOnline() {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager?.activeNetwork ?: throw IOException("offline")
        val capabilities = manager.getNetworkCapabilities(network) ?: throw IOException("offline")
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            throw IOException("offline")
        }
    }

    private suspend fun <T> retry(block: suspend () -> T): T {
        var last: Exception? = null
        repeat(3) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                last = e
                if (e is IOException && e.message == "offline") throw e
                if (attempt < 2) {
                    val retryAfter = (e as? ClientRequestException)
                        ?.response
                        ?.headers
                        ?.get("Retry-After")
                        ?.toLongOrNull()
                    val pause = if (retryAfter != null) {
                        (retryAfter.coerceIn(1, 8)) * 1_000
                    } else {
                        400L * (attempt + 1)
                    }
                    delay(pause)
                }
            }
        }
        throw last ?: IllegalStateException("mailbox")
    }

    @Serializable
    private data class MailAttachment(val url: String? = null)

    @Serializable
    private data class MailEvent(
        val event: String = "",
        val time: Long = 0,
        val message: String? = null,
        val attachment: MailAttachment? = null
    )
}
