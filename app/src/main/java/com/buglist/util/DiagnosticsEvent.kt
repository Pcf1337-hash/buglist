package com.buglist.util

import kotlinx.serialization.Serializable

@Serializable
data class DiagnosticsEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val errorCode: Int = -1,
    val afterBackground: Boolean = false,
    val backgroundSeconds: Long = -1,
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
}
