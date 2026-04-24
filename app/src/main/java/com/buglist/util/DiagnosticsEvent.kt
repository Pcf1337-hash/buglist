package com.buglist.util

import kotlinx.serialization.Serializable

@Serializable
data class DiagnosticsEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val errorCode: Int = -1,
    val afterBackground: Boolean = false,
    val backgroundSeconds: Long = -1,
    /** STRONG = CryptoObject path, FALLBACK = Samsung/WEAK path, UNKNOWN = not set */
    val authPath: String = "",
    /** How many auto-retries happened before this result (background-return scenario) */
    val retryAttempt: Int = 0,
    val sdkInt: Int = android.os.Build.VERSION.SDK_INT,
    val appVersion: String = com.buglist.BuildConfig.VERSION_NAME
)

object DiagEventType {
    const val BIOMETRIC_SUCCESS = "BIOMETRIC_SUCCESS"
    const val BIOMETRIC_FAILED = "BIOMETRIC_FAILED"
    const val BIOMETRIC_CANCELED = "BIOMETRIC_CANCELED"
    const val BIOMETRIC_HW_UNAVAILABLE = "BIOMETRIC_HW_UNAVAILABLE"
    const val KEY_PERMANENTLY_INVALIDATED = "KEY_PERMANENTLY_INVALIDATED"
    const val APP_FOREGROUND = "APP_FOREGROUND"
    const val APP_BACKGROUND = "APP_BACKGROUND"
    const val AUTH_SCREEN_SHOWN = "AUTH_SCREEN_SHOWN"
    const val DB_OPEN_SUCCESS = "DB_OPEN_SUCCESS"
    const val DB_OPEN_FAILED = "DB_OPEN_FAILED"
    const val APP_CRASH = "APP_CRASH"
    const val SESSION_SUMMARY = "SESSION_SUMMARY"
    /** Auto-retry triggered after ERROR_CANCELED (5) on background-return */
    const val BG_RETURN_RETRY = "BG_RETURN_RETRY"
    /** Auth succeeded after one or more background-return auto-retries */
    const val BG_RETURN_SUCCESS = "BG_RETURN_SUCCESS"
}
