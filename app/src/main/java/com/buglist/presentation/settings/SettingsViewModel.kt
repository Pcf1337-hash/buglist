package com.buglist.presentation.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.buglist.util.appDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.data.local.AppDatabase
import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.Person
import com.buglist.domain.model.Tag
import com.buglist.data.remote.UpdateState
import com.buglist.util.DownloadState
import com.buglist.util.UpdateDownloadManager
import com.buglist.domain.repository.TagRepository
import com.buglist.domain.usecase.AddDebtUseCase
import com.buglist.domain.usecase.AddPersonUseCase
import com.buglist.domain.usecase.CheckForUpdateUseCase
import com.buglist.domain.usecase.ExportDataUseCase
import com.buglist.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

private val KEY_CURRENCY = stringPreferencesKey("currency")
private val KEY_AUTO_LOCK = intPreferencesKey("auto_lock_timeout_seconds")
private val KEY_SHOW_DESCRIPTION = booleanPreferencesKey("show_description")

data class SettingsUiData(
    val currency: String = "EUR",
    val autoLockTimeoutSeconds: Int = 60,
    val exportCsv: String? = null,
    val isExporting: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isSeedingData: Boolean = false,
    /** When true, the description/comment field is shown in AddDebtSheet. Default: false. */
    val showDescription: Boolean = false
)

/**
 * ViewModel for the settings screen.
 *
 * All preferences (currency, auto-lock timeout) are persisted to DataStore so they
 * survive process death. Settings are loaded eagerly in [init].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportDataUseCase: ExportDataUseCase,
    private val addPersonUseCase: AddPersonUseCase,
    private val addDebtUseCase: AddDebtUseCase,
    private val sessionManager: SessionManager,
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val appDatabase: AppDatabase,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiData = MutableStateFlow(SettingsUiData())
    val uiData: StateFlow<SettingsUiData> = _uiData.asStateFlow()

    /** All user-defined tags — reactive flow from [TagRepository]. */
    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /**
     * Emitted once after [deleteAllData] completes successfully.
     * The UI collects this to navigate away before the ViewModel is destroyed.
     */
    private val _deleteAllEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteAllEvent: SharedFlow<Unit> = _deleteAllEvent.asSharedFlow()

    fun checkForUpdateManually() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            _updateState.value = checkForUpdateUseCase(forceCheck = true)
        }
    }

    fun onUpdateDismissed() {
        _updateState.value = UpdateState.Idle
        _downloadState.value = DownloadState.Idle
    }

    fun onUpdateSkipped(version: String) {
        viewModelScope.launch {
            checkForUpdateUseCase.skipVersion(version)
            _updateState.value = UpdateState.Idle
            _downloadState.value = DownloadState.Idle
        }
    }

    /**
     * Starts the in-app APK download via [UpdateDownloadManager] and tracks progress
     * in [downloadState]. Mirrors [StartupViewModel.startDownload].
     *
     * @param downloadUrl Direct URL of the APK asset from the GitHub release.
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
     * Returns an install [android.content.Intent] for the downloaded APK, or null
     * if no APK file is present.
     */
    fun buildInstallIntent(): android.content.Intent? =
        UpdateDownloadManager(context).buildInstallIntent()

    /** Resets [downloadState] to [DownloadState.Idle] after a failed download. */
    fun resetDownload() {
        _downloadState.value = DownloadState.Idle
    }

    init {
        // Load persisted settings on startup
        viewModelScope.launch {
            val prefs = context.appDataStore.data.first()
            val currency = prefs[KEY_CURRENCY] ?: "EUR"
            val autoLock = prefs[KEY_AUTO_LOCK] ?: 60
            val showDesc = prefs[KEY_SHOW_DESCRIPTION] ?: false
            _uiData.value = _uiData.value.copy(
                currency = currency,
                autoLockTimeoutSeconds = autoLock,
                showDescription = showDesc
            )
            // Sync session manager with persisted timeout
            sessionManager.autoLockTimeoutMs = autoLock * 1000L
        }
    }

    fun setCurrency(currency: String) {
        _uiData.value = _uiData.value.copy(currency = currency)
        viewModelScope.launch {
            context.appDataStore.edit { it[KEY_CURRENCY] = currency }
        }
    }

    /**
     * Persists whether the description/comment field is visible in AddDebtSheet.
     *
     * @param show true = field visible, false = field hidden (default).
     */
    fun setShowDescription(show: Boolean) {
        _uiData.value = _uiData.value.copy(showDescription = show)
        viewModelScope.launch {
            context.appDataStore.edit { it[KEY_SHOW_DESCRIPTION] = show }
        }
    }

    fun setAutoLockTimeout(seconds: Int) {
        _uiData.value = _uiData.value.copy(autoLockTimeoutSeconds = seconds)
        sessionManager.autoLockTimeoutMs = seconds * 1000L
        viewModelScope.launch {
            context.appDataStore.edit { it[KEY_AUTO_LOCK] = seconds }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _uiData.value = _uiData.value.copy(isExporting = true, exportCsv = null)
            val csv = exportDataUseCase()
            _uiData.value = _uiData.value.copy(isExporting = false, exportCsv = csv)
        }
    }

    fun clearExportCsv() {
        _uiData.value = _uiData.value.copy(exportCsv = null)
    }

    fun showDeleteConfirm() {
        _uiData.value = _uiData.value.copy(showDeleteConfirm = true)
    }

    fun dismissDeleteConfirm() {
        _uiData.value = _uiData.value.copy(showDeleteConfirm = false)
    }

    /**
     * Deletes ALL user data from the database (persons, debt_entries, payments).
     *
     * Uses Room's [RoomDatabase.clearAllTables] which handles table truncation in the
     * correct FK-safe order automatically. After deletion completes, emits [deleteAllEvent]
     * so the UI can navigate back to the dashboard.
     */
    fun deleteAllData() {
        _uiData.value = _uiData.value.copy(showDeleteConfirm = false)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appDatabase.clearAllTables()
            }
            _deleteAllEvent.emit(Unit)
        }
    }

    /**
     * Creates a new tag with the given name.
     *
     * Constraints enforced here (not in domain to keep domain lean for this simple operation):
     * - Name is blank → no-op.
     * - Name exceeds 20 characters → no-op (UI guards this, double-check here).
     * - Maximum 30 tags allowed — silently ignored if the limit is already reached.
     *
     * @param name Display name for the new tag.
     */
    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || trimmed.length > 20) return
        viewModelScope.launch {
            if (allTags.value.size >= 30) return@launch
            tagRepository.insertTag(Tag(name = trimmed))
        }
    }

    /**
     * Permanently deletes a tag.
     * All debt-entry associations are removed automatically via FK CASCADE.
     *
     * @param tag Tag to delete.
     */
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
        }
    }

    /**
     * DEBUG ONLY — generates 50 persons and 500 debt entries for stress testing.
     * Only callable in debug builds (guarded in the UI by BuildConfig.DEBUG).
     */
    fun seedTestData(persons: Int = 50, entries: Int = 500) {
        viewModelScope.launch {
            _uiData.value = _uiData.value.copy(isSeedingData = true)
            val names = listOf(
                "Big Mike", "Lil Ray", "D-Money", "T-Bone", "Slick Will",
                "Ghost", "Ice", "Kane", "Dre", "Smooth", "Rico", "Ace",
                "Blaze", "Crip", "Deuce", "Flex", "G-Man", "Hustle", "Ink", "Jay"
            )
            val descriptions = listOf(
                "Poker night", "Borrowed cash", "Food money", "Gas",
                "Concert tickets", "Drinks", "Rent", "Phone bill",
                "Old debt", "Business deal", null
            )

            val createdIds = mutableListOf<Long>()
            repeat(persons) { i ->
                val name = "${names[i % names.size]} ${i / names.size + 1}".trim()
                    .replace(" 1", "")
                val result = addPersonUseCase(
                    Person(
                        name = name,
                        avatarColor = Random.nextInt()
                    )
                )
                if (result is com.buglist.domain.model.Result.Success) {
                    createdIds.add(result.data)
                }
            }

            if (createdIds.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val dayMs = 24 * 60 * 60 * 1000L
                repeat(entries) { i ->
                    val personId = createdIds[i % createdIds.size]
                    val amount = (Random.nextDouble() * 490 + 10).let {
                        kotlin.math.round(it * 100) / 100.0
                    }
                    addDebtUseCase(
                        DebtEntry(
                            personId = personId,
                            amount = amount,
                            isOwedToMe = Random.nextBoolean(),
                            description = descriptions[i % descriptions.size],
                            date = now - (Random.nextLong(180) * dayMs),
                            currency = "EUR"
                        )
                    )
                }
            }

            _uiData.value = _uiData.value.copy(isSeedingData = false)
        }
    }
}
