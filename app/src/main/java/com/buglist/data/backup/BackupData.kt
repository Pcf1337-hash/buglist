package com.buglist.data.backup

import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.entity.DebtEntryTagCrossRef
import com.buglist.data.local.entity.DividerEntity
import com.buglist.data.local.entity.PaymentEntity
import com.buglist.data.local.entity.PersonEntity
import com.buglist.data.local.entity.TagEntity
import kotlinx.serialization.Serializable

/**
 * Root container for an encrypted BugList backup.
 *
 * This is serialized to JSON, encrypted with AES-256-GCM (key derived via Argon2id),
 * and written to a `.blbak` file. See [BackupCrypto] for the binary file format.
 *
 * Privacy guarantee: all values are structural app data — NO device IDs, NO Android IDs,
 * NO location data, NO PII beyond what the user intentionally stored.
 */
@Serializable
data class BackupPayload(
    /** Room schema version at export time. Used to detect forward-compatibility issues. */
    val schemaVersion: Int = 6,
    val exportedAt: Long = System.currentTimeMillis(),
    val persons: List<PersonBackup> = emptyList(),
    val debtEntries: List<DebtEntryBackup> = emptyList(),
    val payments: List<PaymentBackup> = emptyList(),
    val tags: List<TagBackup> = emptyList(),
    val debtEntryTags: List<CrossRefBackup> = emptyList(),
    val dividers: List<DividerBackup> = emptyList(),
    val settings: SettingsBackup = SettingsBackup()
)

// ─── Per-entity backup classes ────────────────────────────────────────────────
// IDs are always included so FK relationships survive restore without remapping.

@Serializable
data class PersonBackup(
    val id: Long,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val avatarColor: Int = 0,
    val createdAt: Long,
    val sortIndex: Int = Int.MAX_VALUE,
    /** Avatar image path is device-local; will be null after restore on a new device. */
    val avatarImagePath: String? = null
)

@Serializable
data class DebtEntryBackup(
    val id: Long,
    val personId: Long,
    val amount: Double,
    val currency: String = "EUR",
    val isOwedToMe: Boolean,
    val description: String? = null,
    val date: Long,
    val dueDate: Long? = null,
    val status: String = "OPEN",
    val createdAt: Long
)

@Serializable
data class PaymentBackup(
    val id: Long,
    val debtEntryId: Long,
    val amount: Double,
    val note: String? = null,
    val date: Long
)

@Serializable
data class TagBackup(
    val id: Long,
    val name: String,
    val createdAt: Long
)

@Serializable
data class CrossRefBackup(
    val debtEntryId: Long,
    val tagId: Long
)

@Serializable
data class DividerBackup(
    val id: Long,
    val label: String,
    val color: Int,
    val lineStyle: String,
    val sortIndex: Int
)

@Serializable
data class SettingsBackup(
    val currency: String = "EUR",
    val autoLockTimeoutSeconds: Int = 60,
    val showDescription: Boolean = false
)

/** Summary shown to the user before confirming an import. */
data class BackupSummary(
    val personCount: Int,
    val debtCount: Int,
    val paymentCount: Int,
    val tagCount: Int,
    val exportedAt: Long
)

// ─── Mapping extensions ───────────────────────────────────────────────────────

fun PersonEntity.toBackup() = PersonBackup(
    id = id, name = name, phone = phone, notes = notes,
    avatarColor = avatarColor, createdAt = createdAt,
    sortIndex = sortIndex, avatarImagePath = avatarImagePath
)

fun PersonBackup.toEntity() = PersonEntity(
    id = id, name = name, phone = phone, notes = notes,
    avatarColor = avatarColor, createdAt = createdAt,
    sortIndex = sortIndex,
    avatarImagePath = null  // deliberately dropped — path is device-local
)

fun DebtEntryEntity.toBackup() = DebtEntryBackup(
    id = id, personId = personId, amount = amount, currency = currency,
    isOwedToMe = isOwedToMe, description = description, date = date,
    dueDate = dueDate, status = status, createdAt = createdAt
)

fun DebtEntryBackup.toEntity() = DebtEntryEntity(
    id = id, personId = personId, amount = amount, currency = currency,
    isOwedToMe = isOwedToMe, description = description, date = date,
    dueDate = dueDate, status = status, createdAt = createdAt
)

fun PaymentEntity.toBackup() = PaymentBackup(
    id = id, debtEntryId = debtEntryId, amount = amount, note = note, date = date
)

fun PaymentBackup.toEntity() = PaymentEntity(
    id = id, debtEntryId = debtEntryId, amount = amount, note = note, date = date
)

fun TagEntity.toBackup() = TagBackup(id = id, name = name, createdAt = createdAt)

fun TagBackup.toEntity() = TagEntity(id = id, name = name, createdAt = createdAt)

fun DebtEntryTagCrossRef.toBackup() = CrossRefBackup(
    debtEntryId = debtEntryId, tagId = tagId
)

fun CrossRefBackup.toEntity() = DebtEntryTagCrossRef(
    debtEntryId = debtEntryId, tagId = tagId
)

fun DividerEntity.toBackup() = DividerBackup(
    id = id, label = label, color = color, lineStyle = lineStyle, sortIndex = sortIndex
)

fun DividerBackup.toEntity() = DividerEntity(
    id = id, label = label, color = color, lineStyle = lineStyle, sortIndex = sortIndex
)
