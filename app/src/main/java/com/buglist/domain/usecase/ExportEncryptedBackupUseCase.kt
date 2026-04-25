package com.buglist.domain.usecase

import android.content.Context
import com.buglist.data.backup.BackupCrypto
import com.buglist.data.backup.BackupPayload
import com.buglist.data.backup.SettingsBackup
import com.buglist.data.backup.toBackup
import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.DividerDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.dao.PersonDao
import com.buglist.data.local.dao.TagDao
import com.buglist.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Reads all data from the database, serializes it to JSON, encrypts with Argon2id + AES-256-GCM,
 * and writes the result to a `.blbak` file in [Context.getCacheDir].
 *
 * The caller receives the [File] reference and is responsible for sharing it via Intent
 * and deleting the cache file afterwards.
 *
 * @param password User-supplied backup password; used only during this call.
 * @param settings Current app settings (read from ViewModel state — no DataStore I/O here).
 */
class ExportEncryptedBackupUseCase @Inject constructor(
    private val personDao: PersonDao,
    private val debtEntryDao: DebtEntryDao,
    private val paymentDao: PaymentDao,
    private val tagDao: TagDao,
    private val dividerDao: DividerDao,
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke(password: String, settings: SettingsBackup): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Read all data in one IO pass
                val persons = personDao.getAllPersonsSnapshot()
                val debtEntries = debtEntryDao.getAllDebtEntries()
                val payments = paymentDao.getAllPayments()
                val tags = tagDao.getAllTagsSnapshot()
                val crossRefs = tagDao.getAllCrossRefs()
                val dividers = dividerDao.getAllDividersSnapshot()

                // 2. Build payload
                val payload = BackupPayload(
                    persons = persons.map { it.toBackup() },
                    debtEntries = debtEntries.map { it.toBackup() },
                    payments = payments.map { it.toBackup() },
                    tags = tags.map { it.toBackup() },
                    debtEntryTags = crossRefs.map { it.toBackup() },
                    dividers = dividers.map { it.toBackup() },
                    settings = settings
                )

                // 3. Serialize → encrypt
                val json = Json.encodeToString(payload)
                val encrypted = BackupCrypto.encrypt(json.toByteArray(Charsets.UTF_8), password)

                // 4. Write to cacheDir — filename includes timestamp for uniqueness
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(context.cacheDir, "BugList_Backup_$timestamp.blbak")
                file.writeBytes(encrypted)

                file
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error("Backup-Export fehlgeschlagen: ${it.message}", it) }
            )
        }
}
