package com.buglist.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.data.remote.UpdateState
import com.buglist.domain.usecase.CheckForUpdateUseCase
import com.buglist.util.DownloadState
import com.buglist.util.UpdateDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for the one-time startup update check shown on the Dashboard.
 *
 * Lifecycle:
 * 1. [checkOnStartup] is called once when the Dashboard is first composed after auth.
 * 2. If [UpdateState.UpdateAvailable] is found, [updateState] emits the update info.
 * 3. User taps UPDATE → [startDownload] is called; [downloadState] tracks progress.
 * 4. On [DownloadState.Ready] the caller retrieves the install Intent via
 *    [UpdateDownloadManager.buildInstallIntent] and launches it.
 * 5. SKIP / LATER dismiss the dialog ([dismissUpdate] / [skipUpdate]).
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val checkForUpdateUseCase: CheckForUpdateUseCase
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /** True once [checkOnStartup] has been called so it runs at most once per session. */
    private var hasChecked = false

    /**
     * Triggers a background update check using the 24 h cooldown from DataStore.
     * Safe to call multiple times — only executes the network request once per ViewModel
     * instance (i.e. once per Dashboard session).
     */
    fun checkOnStartup() {
        if (hasChecked) return
        hasChecked = true
        viewModelScope.launch {
            val result = checkForUpdateUseCase(forceCheck = false)
            if (result is UpdateState.UpdateAvailable) {
                _updateState.value = result
            }
            // NoConnection / Error / UpToDate: silently ignored on startup
        }
    }

    /**
     * Downloads the APK from [downloadUrl] and updates [downloadState] throughout.
     * After [DownloadState.Ready] the caller should call
     * [buildInstallIntent] and launch the returned Intent.
     *
     * @param downloadUrl Direct URL of the APK asset from the GitHub Release.
     */
    fun startDownload(downloadUrl: String) {
        viewModelScope.launch {
            val manager = UpdateDownloadManager(context)
            manager.downloadApk(downloadUrl) { state ->
                _downloadState.value = state
            }
        }
    }

    /**
     * Returns an install [android.content.Intent] for the APK that was downloaded, or null
     * if no APK is available.
     */
    fun buildInstallIntent(): android.content.Intent? =
        UpdateDownloadManager(context).buildInstallIntent()

    /** Dismisses the update dialog without skipping (will reappear after 24 h). */
    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Marks [version] as skipped in DataStore — the dialog will not reappear for this version.
     *
     * @param version The remote version string (without leading "v").
     */
    fun skipUpdate(version: String) {
        viewModelScope.launch {
            checkForUpdateUseCase.skipVersion(version)
            _updateState.value = UpdateState.Idle
            _downloadState.value = DownloadState.Idle
        }
    }

    /** Resets [downloadState] to [DownloadState.Idle] (e.g. after a failed download). */
    fun resetDownload() {
        _downloadState.value = DownloadState.Idle
    }
}
