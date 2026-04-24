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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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

    // Session counters — reset after each sendSessionSummary()
    private val sessionStartTimestamp = AtomicLong(System.currentTimeMillis())
    private val authSuccessCount = AtomicInteger(0)
    private val authFailCount = AtomicInteger(0)
    private val authCancelCount = AtomicInteger(0)
    private val dbOpenFailed = AtomicBoolean(false)

    private val criticalTypes = setOf(
        DiagEventType.BIOMETRIC_FAILED,
        DiagEventType.BIOMETRIC_CANCELED,
        DiagEventType.KEY_PERMANENTLY_INVALIDATED,
        DiagEventType.DB_OPEN_FAILED,
        DiagEventType.APP_CRASH
    )

    /** Adds an event to the local queue (max 200). Uploads critical events automatically. */
    fun record(event: DiagnosticsEvent) {
        events.offer(event)
        while (events.size > 200) events.poll()

        // Update session counters
        when (event.eventType) {
            DiagEventType.BIOMETRIC_SUCCESS -> authSuccessCount.incrementAndGet()
            DiagEventType.BIOMETRIC_FAILED -> authFailCount.incrementAndGet()
            DiagEventType.BIOMETRIC_CANCELED -> authCancelCount.incrementAndGet()
            DiagEventType.DB_OPEN_FAILED -> dbOpenFailed.set(true)
        }

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

    /**
     * Sends a low-priority session summary to ntfy.sh and resets session counters.
     * Call this from Activity.onPause().
     */
    fun sendSessionSummary() {
        val sessionSeconds = (System.currentTimeMillis() - sessionStartTimestamp.get()) / 1000
        val ok = authSuccessCount.getAndSet(0)
        val fail = authFailCount.getAndSet(0)
        val cancel = authCancelCount.getAndSet(0)
        val dbFail = dbOpenFailed.getAndSet(false)
        sessionStartTimestamp.set(System.currentTimeMillis())

        val msg = buildString {
            append("SESSION")
            append(" | auths:${ok + fail + cancel}")
            append(" ok:$ok fail:$fail cancel:$cancel")
            append(" | fg:${sessionSeconds}s")
            if (dbFail) append(" | db:FAIL") else append(" | db:ok")
            append(" | sdk:${android.os.Build.VERSION.SDK_INT}")
            append(" | ver:${BuildConfig.VERSION_NAME}")
        }

        scope.launch {
            runCatching {
                val payload = """{"topic":"BugListLogs","title":"📊 Session","message":"$msg","priority":2,"tags":["chart_with_upwards_trend"]}"""
                httpClient.post(BuildConfig.NTFY_TOPIC_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            }.onFailure { e ->
                if (BuildConfig.DEBUG) Log.d("DiagMgr", "Summary upload skipped: ${e.message}")
            }
        }
    }

    /**
     * Installs an UncaughtExceptionHandler that fires a critical ntfy.sh push on crash.
     * Only the exception class name is transmitted — no stack trace, no user data.
     * Call once from Application.onCreate() after Hilt injection.
     */
    fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                // Only the class simpleName — never the message (may contain user-visible text)
                val safeClass = throwable::class.java.simpleName
                val bgSecs = secondsSinceBackground()
                val msg = "CRASH: $safeClass | bg:${bgSecs > 0} | bgS:${bgSecs}s | sdk:${android.os.Build.VERSION.SDK_INT} | ver:${BuildConfig.VERSION_NAME}"
                val payload = """{"topic":"BugListLogs","title":"💥 App Crash","message":"$msg","priority":5,"tags":["rotating_light"]}"""

                // Synchronous HTTP — no coroutines available during crash
                val url = java.net.URL(BuildConfig.NTFY_TOPIC_URL)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.outputStream.use { it.write(payload.toByteArray()) }
                conn.inputStream.close()
                conn.disconnect()
            }
            // Always forward to the original handler (e.g. system crash dialog / Firebase)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

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
