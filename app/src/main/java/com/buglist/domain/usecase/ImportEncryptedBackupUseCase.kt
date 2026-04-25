package com.buglist.domain.usecase

import android.content.Context
import android.net.Uri
import com.buglist.data.backup.BackupCrypto
import com.buglist.data.backup.BackupPayload
import com.buglist.data.backup.BackupSummary
import com.buglist.data.backup.toEntity
import com.buglist.data.local.AppDatabase
import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.DividerDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.dao.PersonDao
import com.buglist.data.local.dao.TagDao
import com.buglist.data.local.entity.DebtEntryTagCrossRef
import com.buglist.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.crypto.AEADBadTagException
import javax.inject.Inject

/**
 * Handles the two-phase encrypted backup restore:
 *
 * **Phase 1 — [parseAndValidate]:** Reads the file URI, decrypts, and parses the JSON payload.
 * Returns a [BackupSummary] for user confirmation. The parsed [BackupPayload] is returned to the
 * caller (ViewModel) which holds it until the user confirms.
 *
 * **Phase 2 — [applyBackup]:** Clears all existing data and inserts the backup in FK-safe order.
 * Settings restoration is handled by the ViewModel after this call succeeds.
 *
 * Thread safety: both methods run exclusively on [Dispatchers.IO].
 */
class ImportEncryptedBackupUseCase @Inject constructor(
    private val appDatabase: AppDatabase,
    private val personDao: PersonDao,
    private val debtEntryDao: DebtEntryDao,
    private val paymentDao: PaymentDao,
    private val tagDao: TagDao,
    private val dividerDao: DividerDao,
    @ApplicationContext private val context: Context
) {

    /**
     * Reads [uri], decrypts with [password], and parses the JSON payload.
     *
     * @return [Result.Success] with the parsed [BackupPayload] and a summary for display,
     *         or [Result.Error] with a user-facing message.
     *         Returns "Falsches Passwort" on [AEADBadTagException] (wrong key or corrupted file).
     */
    suspend fun parseAndValidate(
        uri: Uri,
        password: String
    ): Result<Pair<BackupPayload, BackupSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            // Read file bytes via ContentResolver (works with SAF, Downloads, Drive, etc.)
            val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Datei konnte nicht gelesen werden")

            // Decrypt — throws AEADBadTagException if password wrong
            val jsonBytes = BackupCrypto.decrypt(fileBytes, password)
            val payload = Json.decodeFromString<BackupPayload>(jsonBytes.toString(Charsets.UTF_8))

            val summary = BackupSummary(
                personCount = payload.persons.size,
                debtCount = payload.debtEntries.size,
                paymentCount = payload.payments.size,
                tagCount = payload.tags.size,
                exportedAt = payload.exportedAt
            )

            Pair(payload, summary)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                val message = when (e) {
                    is AEADBadTagException -> "Falsches Passwort oder beschädigte Datei"
                    is IllegalArgumentException -> "Ungültige Backup-Datei: ${e.message}"
                    else -> "Import fehlgeschlagen: ${e.message}"
                }
                Result.Error(message, e)
            }
        )
    }

    /**
     * Clears all existing app data and restores the [payload].
     *
     * Insert order respects FK constraints:
     * persons → tags → debt_entries → payments → debt_entry_tags → dividers
     *
     * After this returns successfully, the caller should restore DataStore settings
     * from [BackupPayload.settings].
     */
    suspend fun applyBackup(payload: BackupPayload): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Wipe everything — clearAllTables handles FK order automatically
                appDatabase.clearAllTables()

                // 2. Re-insert in FK-safe order (parents before children)
                personDao.insertAll(payload.persons.map { it.toEntity() })
                tagDao.insertAllTags(payload.tags.map { it.toEntity() })
                debtEntryDao.insertAll(payload.debtEntries.map { it.toEntity() })
                paymentDao.insertAll(payload.payments.map { it.toEntity() })

                // CrossRefs — tagDao.insertCrossRefs already uses REPLACE strategy
                if (payload.debtEntryTags.isNotEmpty()) {
                    tagDao.insertCrossRefs(
                        payload.debtEntryTags.map {
                            DebtEntryTagCrossRef(
                                debtEntryId = it.debtEntryId,
                                tagId = it.tagId
                            )
                        }
                    )
                }

                dividerDao.insertAll(payload.dividers.map { it.toEntity() })
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { Result.Error("Restore fehlgeschlagen: ${it.message}", it) }
            )
        }
}
