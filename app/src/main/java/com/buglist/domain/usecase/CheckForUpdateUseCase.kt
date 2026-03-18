package com.buglist.domain.usecase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.buglist.BuildConfig
import com.buglist.data.remote.GithubRelease
import com.buglist.data.remote.UpdateRepository
import com.buglist.data.remote.UpdateState
import com.buglist.util.UpdateConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "update_prefs"
)

class CheckForUpdateUseCase @Inject constructor(
    private val updateRepository: UpdateRepository,
    @ApplicationContext private val context: Context
) {
    private val lastCheckKey = longPreferencesKey(UpdateConfig.PREF_LAST_UPDATE_CHECK)
    private val skippedVersionKey = stringPreferencesKey(UpdateConfig.PREF_SKIPPED_VERSION)

    suspend operator fun invoke(forceCheck: Boolean = false): UpdateState {
        if (!forceCheck) {
            val lastCheck = context.updateDataStore.data.first()[lastCheckKey] ?: 0L
            val elapsed = System.currentTimeMillis() - lastCheck
            if (elapsed < UpdateConfig.CHECK_INTERVAL_MS) {
                return UpdateState.Idle
            }
        }

        context.updateDataStore.edit { it[lastCheckKey] = System.currentTimeMillis() }

        return try {
            val release = updateRepository.fetchLatestRelease().getOrThrow()
            val currentVersion = BuildConfig.VERSION_NAME
            val remoteVersion = updateRepository.normalizeVersion(release.tagName)

            val skippedVersion = context.updateDataStore.data.first()[skippedVersionKey] ?: ""
            if (skippedVersion == remoteVersion && !forceCheck) {
                return UpdateState.Idle
            }

            if (updateRepository.isNewerVersion(currentVersion, remoteVersion)) {
                val apkAsset = updateRepository.findApkAsset(release)
                UpdateState.UpdateAvailable(
                    currentVersion = currentVersion,
                    newVersion = remoteVersion,
                    releaseNotes = release.body?.take(500),
                    downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl,
                    releasePageUrl = release.htmlUrl
                )
            } else {
                UpdateState.UpToDate
            }
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException) {
                UpdateState.NoConnection
            } else {
                UpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun skipVersion(version: String) {
        context.updateDataStore.edit { it[skippedVersionKey] = version }
    }
}
