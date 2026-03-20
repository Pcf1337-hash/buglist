package com.buglist.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.buglist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Represents the current state of an APK download initiated by [UpdateDownloadManager].
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progressFraction: Float) : DownloadState()
    object Ready : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * Handles downloading an APK update via Android's system [DownloadManager] into the app's
 * cache directory. After a successful download the caller receives a [DownloadState.Ready]
 * and can call [buildInstallIntent] to get an install [android.content.Intent].
 *
 * Rules enforced:
 * - APK is saved to the app's external files directory via [setDestinationInExternalFilesDir].
 *   Android's DownloadManager rejects internal `file://` URIs (data/data/…/cache); it only
 *   accepts external storage paths. [Context.getExternalFilesDir] is app-private, needs no
 *   WRITE_EXTERNAL_STORAGE permission, and is cleaned up on uninstall. (L-042)
 * - FileProvider authority `${applicationId}.fileprovider` is used for the install Intent.
 * - Files < 100 KB after download are rejected ([DownloadState.Error]) to guard against
 *   truncated or error-page downloads.
 * - [pollProgress] polls [DownloadManager] every 500 ms and suspends until the download
 *   finishes (success or failure).
 */
class UpdateDownloadManager(private val context: Context) {

    companion object {
        private const val APK_FILENAME = "buglist_update.apk"
        private const val MIN_APK_SIZE_BYTES = 100 * 1024L  // 100 KB minimum
        private const val POLL_INTERVAL_MS = 500L
    }

    private val systemDownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // External app-private directory: /sdcard/Android/data/com.buglist/files/
    // DownloadManager requires an external storage path — internal cacheDir is rejected. (L-042)
    private val apkFile: File
        get() = File(context.getExternalFilesDir(null), APK_FILENAME)

    /**
     * Starts the download of the APK at [url] and polls for progress until completion.
     * Emits [DownloadState] updates via [onProgress].
     *
     * This function is safe to call on a background coroutine ([Dispatchers.IO] recommended).
     *
     * @param url           Direct download URL of the APK asset.
     * @param onProgress    Callback invoked with each [DownloadState] update.
     */
    suspend fun downloadApk(
        url: String,
        onProgress: (DownloadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        // Remove stale APK from a previous attempt
        if (apkFile.exists()) apkFile.delete()

        val downloadId = try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("BugList Update")
                setDescription("Downloading update…")
                // setDestinationInExternalFilesDir avoids the "unsupported path" crash:
                // DownloadManager only writes to external storage, not internal cacheDir. (L-042)
                setDestinationInExternalFilesDir(context, null, APK_FILENAME)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setMimeType("application/vnd.android.package-archive")
            }
            systemDownloadManager.enqueue(request)
        } catch (e: Exception) {
            onProgress(DownloadState.Error("Download starten fehlgeschlagen: ${e.message}"))
            return@withContext
        }

        pollProgress(downloadId, onProgress)
    }

    /**
     * Polls [DownloadManager] for the download identified by [downloadId] until it is
     * complete (success or error). Emits [DownloadState.Downloading] updates during the poll.
     */
    private suspend fun pollProgress(
        downloadId: Long,
        onProgress: (DownloadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = systemDownloadManager.query(query)

            if (cursor == null || !cursor.moveToFirst()) {
                cursor?.close()
                onProgress(DownloadState.Error("Download-Abfrage fehlgeschlagen"))
                return@withContext
            }

            val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesDownloadedColumn =
                cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val bytesTotalColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val reasonColumn = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

            val status = cursor.getInt(statusColumn)
            val bytesDownloaded = cursor.getLong(bytesDownloadedColumn)
            val bytesTotal = cursor.getLong(bytesTotalColumn)
            val reason = cursor.getInt(reasonColumn)
            cursor.close()

            when (status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_PAUSED -> {
                    val fraction = if (bytesTotal > 0) {
                        bytesDownloaded.toFloat() / bytesTotal.toFloat()
                    } else {
                        0f
                    }
                    onProgress(DownloadState.Downloading(fraction))
                    delay(POLL_INTERVAL_MS)
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    // Validate minimum file size to reject error-page responses
                    val actualSize = apkFile.length()
                    if (actualSize < MIN_APK_SIZE_BYTES) {
                        apkFile.delete()
                        onProgress(
                            DownloadState.Error(
                                "Heruntergeladene Datei zu klein (${actualSize / 1024} KB). " +
                                    "Bitte manuell herunterladen."
                            )
                        )
                    } else {
                        onProgress(DownloadState.Ready)
                    }
                    return@withContext
                }

                DownloadManager.STATUS_FAILED -> {
                    apkFile.delete()
                    onProgress(DownloadState.Error("Download fehlgeschlagen (Fehler $reason)"))
                    return@withContext
                }

                else -> {
                    onProgress(DownloadState.Error("Unbekannter Download-Status: $status"))
                    return@withContext
                }
            }
        }
    }

    /**
     * Builds an install [android.content.Intent] for the downloaded APK.
     * Returns null if the APK file does not exist.
     *
     * Uses [FileProvider] to serve the file content — no legacy storage permission required.
     */
    fun buildInstallIntent(): android.content.Intent? {
        if (!apkFile.exists()) return null
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apkFile
        )
        return android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
