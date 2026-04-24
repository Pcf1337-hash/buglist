package com.buglist.util

import android.util.Log
import com.buglist.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class DiagnosticsManager @Inject constructor(
    @Named("diagnostics") private val httpClient: HttpClient
) {

    private val events = ConcurrentLinkedQueue<DiagnosticsEvent>()
    private val lastBackgroundTimestamp = AtomicLong(0L)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val criticalTypes = setOf(
        DiagEventType.BIOMETRIC_FAILED,
        DiagEventType.BIOMETRIC_CANCELED,
        DiagEventType.KEY_PERMANENTLY_INVALIDATED
    )

    /** Adds an event to the local queue (max 200). Uploads critical events automatically. */
    fun record(event: DiagnosticsEvent) {
        events.offer(event)
        while (events.size > 200) events.poll()
        if (event.eventType in criticalTypes) uploadEvent(event)
    }

    /** Call this in Activity.onPause() to track background entry time. */
    fun markBackground() {
        lastBackgroundTimestamp.set(System.currentTimeMillis())
    }

    /** Returns seconds since last background, or 0 if app was never backgrounded. */
    fun secondsSinceBackground(): Long {
        val ts = lastBackgroundTimestamp.get()
        return if (ts == 0L) 0L else (System.currentTimeMillis() - ts) / 1000
    }

    /** Returns all recorded events as a JSON string for manual export. */
    fun exportAsJson(): String = Json.encodeToString(events.toList())

    private fun uploadEvent(event: DiagnosticsEvent) {
        scope.launch {
            runCatching {
                val msg = buildString {
                    append(event.eventType)
                    if (event.errorCode >= 0) append(" | ec:${event.errorCode}")
                    append(" | bg:${event.afterBackground}")
                    if (event.backgroundSeconds >= 0) append(" | bgS:${event.backgroundSeconds}s")
                    append(" | sdk:${event.sdkInt}")
                    append(" | ver:${event.appVersion}")
                }
                val payload = """{"topic":"BugListLogs","title":"🔴 BugList Diagnose","message":"$msg","priority":4,"tags":["warning"]}"""
                httpClient.post(BuildConfig.NTFY_TOPIC_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            }.onFailure { e ->
                if (BuildConfig.DEBUG) Log.d("DiagMgr", "Upload skipped: ${e.message}")
            }
        }
    }
}
