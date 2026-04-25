package com.buglist.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.buglist.util.appDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.data.backup.BackupPayload
import com.buglist.data.backup.BackupSummary
import com.buglist.data.backup.SettingsBackup
import com.buglist.data.local.AppDatabase
import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.Person
import com.buglist.domain.model.Result
import com.buglist.domain.model.Tag
import com.buglist.data.remote.UpdateState
import com.buglist.util.DownloadState
import com.buglist.util.UpdateDownloadManager
import com.buglist.domain.repository.TagRepository
import com.buglist.domain.usecase.AddDebtUseCase
import com.buglist.domain.usecase.AddPersonUseCase
import com.buglist.domain.usecase.CheckForUpdateUseCase
import com.buglist.domain.usecase.ExportDataUseCase
import com.buglist.domain.usecase.ExportEncryptedBackupUseCase
import com.buglist.domain.usecase.ImportEncryptedBackupUseCase
import com.buglist.security.SessionManager
import com.buglist.util.DiagnosticsManager
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
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

private val KEY_CURRENCY = stringPreferencesKey("currency")
private val KEY_AUTO_LOCK = intPreferencesKey("auto_lock_timeout_seconds")
private val KEY_SHOW_DESCRIPTION = booleanPreferencesKey("show_description")
private val KEY_DIAGNOSTICS_PUSH = booleanPreferencesKey("diagnostics_push_enabled")

data class SettingsUiData(
    val currency: String = "EUR",
    val autoLockTimeoutSeconds: Int = 60,
    val exportCsv: String? = null,
    val isExporting: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isSeedingData: Boolean = false,
    /** When true, the description/comment field is shown in AddDebtSheet. Default: false. */
    val showDescription: Boolean = false,
    /**
     * When true, the app sends real-time debug pushes to ntfy.sh (Auth events, crashes, etc.).
     * Default true — can be disabled per-device from Settings.
     */
    val diagnosticsPushEnabled: Boolean = true,
    /** True while Argon2 KDF + AES-GCM encryption is running for backup export. */
    val isExportingBackup: Boolean = false,
    /**
     * Set to the cache File after a successful backup export.
     * The Screen observes this to fire a Share Intent, then calls [SettingsViewModel.clearBackupExportFile].
     */
    val backupExportFile: File? = null,
    /** Non-null while import confirmation dialog is shown (shows person/debt counts). */
    val importSummary: BackupSummary? = null,
    /** Non-null when import decryption or validation failed. */
    val importError: String? = null,
    /** True while Argon2 decryption + DB restore is running. */
    val isImportingBackup: Boolean = false
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
    private val tagRepository: TagRepository,
    private val diagnosticsManager: DiagnosticsManager,
    private val exportEncryptedBackupUseCase: ExportEncryptedBackupUseCase,
    private val importEncryptedBackupUseCase: ImportEncryptedBackupUseCase
) : ViewModel() {

    /** Holds the decrypted-but-not-yet-applied payload during import confirmation phase. */
    private var pendingBackupPayload: BackupPayload? = null

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
            val diagPush = prefs[KEY_DIAGNOSTICS_PUSH] ?: true
            _uiData.value = _uiData.value.copy(
                currency = currency,
                autoLockTimeoutSeconds = autoLock,
                showDescription = showDesc,
                diagnosticsPushEnabled = diagPush
            )
            // Sync session manager with persisted timeout
            sessionManager.autoLockTimeoutMs = autoLock * 1000L
            // Sync diagnostics manager with persisted toggle state
            diagnosticsManager.setEnabled(diagPush)
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

    /**
     * Enables or disables real-time ntfy.sh debug push notifications.
     * Persisted in DataStore; immediately applied to [DiagnosticsManager].
     */
    fun setDiagnosticsPush(enabled: Boolean) {
        _uiData.value = _uiData.value.copy(diagnosticsPushEnabled = enabled)
        diagnosticsManager.setEnabled(enabled)
        viewModelScope.launch {
            context.appDataStore.edit { it[KEY_DIAGNOSTICS_PUSH] = enabled }
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

    // ─── Encrypted Backup ────────────────────────────────────────────────────

    /**
     * Exports all app data as an encrypted `.blbak` file.
     *
     * Runs Argon2id KDF + AES-256-GCM on IO. On success, [SettingsUiData.backupExportFile]
     * is set to the cache File — the Screen observes this to fire a Share Intent.
     *
     * @param password User-supplied backup password (min 4 chars, enforced in UI).
     */
    fun exportBackup(password: String) {
        viewModelScope.launch {
            _uiData.value = _uiData.value.copy(isExportingBackup = true, backupExportFile = null)
            val settings = SettingsBackup(
                currency = _uiData.value.currency,
                autoLockTimeoutSeconds = _uiData.value.autoLockTimeoutSeconds,
                showDescription = _uiData.value.showDescription
            )
            val result = exportEncryptedBackupUseCase(password, settings)
            _uiData.value = when (result) {
                is com.buglist.domain.model.Result.Success ->
                    _uiData.value.copy(isExportingBackup = false, backupExportFile = result.data)
                is com.buglist.domain.model.Result.Error ->
                    _uiData.value.copy(isExportingBackup = false, importError = result.message)
            }
        }
    }

    /** Called by the Screen after the Share Intent has been fired. Clears the export file reference. */
    fun clearBackupExportFile() {
        val file = _uiData.value.backupExportFile
        _uiData.value = _uiData.value.copy(backupExportFile = null)
        // Clean up cache file asynchronously
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            file?.delete()
        }
    }

    /**
     * Phase 1 of import: decrypts the file at [uri] with [password] and validates the payload.
     *
     * On success, the payload is held in [pendingBackupPayload] and [SettingsUiData.importSummary]
     * is set to show the confirmation dialog. Call [confirmImport] to apply, or [dismissImportResult]
     * to cancel.
     *
     * @param uri   Content URI from SAF file picker.
     * @param password User-supplied password to decrypt the backup.
     */
    fun validateImportFile(uri: Uri, password: String) {
        viewModelScope.launch {
            _uiData.value = _uiData.value.copy(isImportingBackup = true, importError = null, importSummary = null)
            val result = importEncryptedBackupUseCase.parseAndValidate(uri, password)
            when (result) {
                is com.buglist.domain.model.Result.Success -> {
                    pendingBackupPayload = result.data.first
                    _uiData.value = _uiData.value.copy(
                        isImportingBackup = false,
                        importSummary = result.data.second
                    )
                }
                is com.buglist.domain.model.Result.Error -> {
                    _uiData.value = _uiData.value.copy(
                        isImportingBackup = false,
                        importError = result.message
                    )
                }
            }
        }
    }

    /**
     * Phase 2 of import: applies the [pendingBackupPayload] (wipe + restore) and restores
     * DataStore settings from the payload.
     *
     * Must only be called when [SettingsUiData.importSummary] is non-null (user confirmed dialog).
     * Emits [deleteAllEvent] on success so the Screen navigates back to Dashboard.
     */
    fun confirmImport() {
        val payload = pendingBackupPayload ?: return
        _uiData.value = _uiData.value.copy(isImportingBackup = true, importSummary = null)
        viewModelScope.launch {
            val result = importEncryptedBackupUseCase.applyBackup(payload)
            pendingBackupPayload = null
            when (result) {
                is com.buglist.domain.model.Result.Success -> {
                    // Restore DataStore settings from backup payload
                    val s = payload.settings
                    context.appDataStore.edit { prefs ->
                        prefs[KEY_CURRENCY] = s.currency
                        prefs[KEY_AUTO_LOCK] = s.autoLockTimeoutSeconds
                        prefs[KEY_SHOW_DESCRIPTION] = s.showDescription
                    }
                    _uiData.value = _uiData.value.copy(
                        isImportingBackup = false,
                        currency = s.currency,
                        autoLockTimeoutSeconds = s.autoLockTimeoutSeconds,
                        showDescription = s.showDescription
                    )
                    sessionManager.autoLockTimeoutMs = s.autoLockTimeoutSeconds * 1000L
                    _deleteAllEvent.emit(Unit)
                }
                is com.buglist.domain.model.Result.Error -> {
                    _uiData.value = _uiData.value.copy(
                        isImportingBackup = false,
                        importError = result.message
                    )
                }
            }
        }
    }

    /** Clears import error message or cancels the pending import confirmation. */
    fun dismissImportResult() {
        pendingBackupPayload = null
        _uiData.value = _uiData.value.copy(
            importSummary = null,
            importError = null,
            isImportingBackup = false
        )
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Returns all recorded diagnostic events as a JSON string. */
    fun exportDiagnostics(): String = diagnosticsManager.exportAsJson()

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
