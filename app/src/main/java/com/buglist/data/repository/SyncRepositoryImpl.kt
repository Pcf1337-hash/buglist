package com.buglist.data.repository

import androidx.room.withTransaction
import com.buglist.data.local.AppDatabase
import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.dao.SyncDao
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.entity.DebtSyncEntity
import com.buglist.data.local.entity.PairingEntity
import com.buglist.data.local.entity.PaymentEntity
import com.buglist.data.local.entity.PaymentSyncEntity
import com.buglist.data.local.entity.SyncTombstoneEntity
import com.buglist.data.sync.ChangeSignal
import com.buglist.data.sync.NtfyMailbox
import com.buglist.data.sync.PairingCode
import com.buglist.data.sync.SnapshotCrypto
import com.buglist.data.sync.SyncNote
import com.buglist.data.sync.WireBook
import com.buglist.data.sync.toLocalBook
import com.buglist.data.sync.toWire
import com.buglist.domain.sync.PeerMessage
import com.buglist.domain.sync.PhoneSync
import com.buglist.domain.sync.SyncProblem
import com.buglist.domain.sync.handshake
import com.buglist.domain.sync.visibleProblem
import com.buglist.domain.model.PairingLink
import com.buglist.domain.model.debtSerial
import com.buglist.domain.model.Result
import com.buglist.domain.model.SyncRole
import com.buglist.domain.repository.SyncRepository
import com.buglist.domain.sync.BookDebt
import com.buglist.domain.sync.BookPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncDao: SyncDao,
    private val debtEntryDao: DebtEntryDao,
    private val paymentDao: PaymentDao,
    private val mailbox: NtfyMailbox,
    private val changeSignal: ChangeSignal,
    private val database: AppDatabase
) : SyncRepository {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val locks = ConcurrentHashMap<Long, Mutex>()

    override fun observe(personId: Long): Flow<PairingLink?> =
        syncDao.observePairing(personId).map { entity ->
            entity?.let {
                PairingLink(
                    role = SyncRole.fromStored(it.role),
                    wantedHost = it.wantedRole?.let { role -> role == "HOST" },
                    peerHost = it.peerRole?.let { role -> role == "HOST" },
                    peerSeen = it.peerSeenAt > 0L
                )
            }
        }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun startCoupling(personId: Long, bothCanEdit: Boolean): String {
        val existing = syncDao.getPairing(personId)
        if (existing != null) {
            return PairingCode.encode(existing.roomId, existing.keyMaterial, existing.role == "BOTH")
        }
        val pairing = PairingEntity(
            personId = personId,
            role = "PENDING",
            roomId = PairingCode.randomRoomId(),
            keyMaterial = PairingCode.randomKey(),
            deviceId = PairingCode.randomDeviceId(),
            lastPushedHash = "",
            linkedAt = System.currentTimeMillis()
        )
        syncDao.upsertPairing(pairing)
        reconcile(personId)
        return PairingCode.encode(pairing.roomId, pairing.keyMaterial, false)
    }

    override suspend fun acceptCode(personId: Long, code: String): Result<PairingLink> {
        val offer = PairingCode.decode(code)
            ?: return Result.Error("Das ist kein BugList-Kopplungscode")
        syncDao.upsertPairing(
            PairingEntity(
                personId = personId,
                role = "PENDING",
                roomId = offer.roomId,
                keyMaterial = offer.keyBase64Url,
                deviceId = PairingCode.randomDeviceId(),
                lastPushedHash = "",
                linkedAt = System.currentTimeMillis()
            )
        )
        val synced = reconcile(personId)
        val link = observeLink(personId)
        return if (synced is Result.Error && synced.message != WAITING_FOR_BOOK) {
            Result.Error(synced.message)
        } else {
            Result.Success(link)
        }
    }

    override suspend fun claimRole(personId: Long, host: Boolean): Result<Unit> {
        val pairing = syncDao.getPairing(personId) ?: return Result.Error("Noch nicht verbunden")
        syncDao.upsertPairing(pairing.copy(wantedRole = if (host) "HOST" else "GUEST"))
        return reconcile(personId)
    }

    override suspend fun unlink(personId: Long) {
        syncDao.deletePairing(personId)
    }

    override suspend fun reconcile(personId: Long): Result<Unit> {
        val mutex = locks.getOrPut(personId) { Mutex() }
        return mutex.withLock {
            val pairing = syncDao.getPairing(personId) ?: return@withLock Result.Success(Unit)
            val fetched = try {
                readNotes(pairing)
            } catch (_: Exception) {
                return@withLock Result.Error(problemText(SyncProblem.OFFLINE))
            }
            var book: List<BookDebt>? = null
            val inbox = fetched.map { note ->
                val kind = when (note.t) {
                    "role" -> "role"
                    "hello" -> "hello"
                    else -> "book"
                }
                if (kind == "book" && note.from.isNotBlank() && note.from != pairing.deviceId) {
                    book = note.debts.map { it.toLocalBook() }
                }
                PeerMessage(from = note.from, kind = kind, role = note.role)
            }
            val result = handshake(
                PhoneSync(
                    deviceId = pairing.deviceId,
                    wanted = pairing.wantedRole,
                    role = pairing.role,
                    peerRole = pairing.peerRole,
                    peerSeen = pairing.peerSeenAt > 0L
                ),
                inbox
            )
            val seenAt = if (result.phone.peerSeen) {
                pairing.peerSeenAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            } else {
                0L
            }
            val updated = pairing.copy(
                role = result.phone.role,
                peerRole = result.phone.peerRole,
                peerSeenAt = seenAt
            )
            syncDao.upsertPairing(updated)
            for (message in result.outgoing) {
                val note = when (message.kind) {
                    "role" -> SyncNote(t = "role", from = pairing.deviceId, role = message.role)
                    else -> SyncNote(t = "hello", from = pairing.deviceId)
                }
                // Polling must not consume the mailbox's publish quota. Keep the
                // newest own confirmation until it expires; a changed role still
                // needs a new message even if an older matching role is cached.
                val retained = fetched.lastOrNull { it.from == note.from && it.t == note.t }
                if (retained != null && retained.role == note.role) continue
                publishNote(updated, note)?.let { return@withLock Result.Error(it) }
            }
            if (result.pushBook) {
                // The mailbox expires cached messages. A saved hash alone does not
                // mean a new or returning guest can still download this book.
                val bookStillCached = fetched.any { it.t == "book" && it.from == pairing.deviceId }
                push(updated, force = !bookStillCached)?.let { return@withLock Result.Error(it) }
            }
            val received = book
            if (result.takeBook && received != null) {
                try {
                    replaceLocalBook(updated.personId, received)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    return@withLock Result.Error("Stand konnte nicht übernommen werden")
                }
                changeSignal.bump()
            }
            val problem = visibleProblem(fetchFailed = false, problem = result.problem)
            if (problem != null) Result.Error(problemText(problem)) else Result.Success(Unit)
        }
    }

    private suspend fun observeLink(personId: Long): PairingLink {
        val row = syncDao.getPairing(personId)
        return PairingLink(
            role = SyncRole.fromStored(row?.role ?: "PENDING"),
            wantedHost = row?.wantedRole?.let { it == "HOST" },
            peerHost = row?.peerRole?.let { it == "HOST" },
            peerSeen = (row?.peerSeenAt ?: 0L) > 0L
        )
    }

    private suspend fun readNotes(pairing: PairingEntity): List<SyncNote> =
        mailbox.messages(PairingCode.topicFor(pairing.roomId)).mapNotNull { payload ->
            val plain = SnapshotCrypto.decrypt(payload, pairing.keyMaterial) ?: return@mapNotNull null
            runCatching { json.decodeFromString<SyncNote>(plain.decodeToString()) }.getOrNull()
        }

    private fun problemText(problem: SyncProblem): String = when (problem) {
        SyncProblem.BOTH_HOST -> "Ihr seid beide Host. Einer muss nur zusehen."
        SyncProblem.BOTH_VIEW -> "Einer muss die Liste führen."
        SyncProblem.NO_BOOK -> WAITING_FOR_BOOK
        SyncProblem.OFFLINE -> "Stand gerade nicht erreichbar"
    }

    private suspend fun publishNote(pairing: PairingEntity, note: SyncNote): String? {
        val payload = SnapshotCrypto.encrypt(
            json.encodeToString(note).encodeToByteArray(),
            pairing.keyMaterial
        )
        return try {
            mailbox.publish(PairingCode.topicFor(pairing.roomId), payload)
            null
        } catch (_: Exception) {
            "Stand gerade nicht erreichbar"
        }
    }

    private suspend fun push(pairing: PairingEntity, force: Boolean): String? {
        val book = loadBook(pairing.personId)
        val wire = WireBook(
            from = pairing.deviceId,
            debts = book.map { it.toWire() }
        )
        val bytes = json.encodeToString(wire).encodeToByteArray()
        val hash = SnapshotCrypto.sha256(bytes)
        if (!force && hash == pairing.lastPushedHash) return null
        val payload = SnapshotCrypto.encrypt(bytes, pairing.keyMaterial)
        return try {
            mailbox.publish(PairingCode.topicFor(pairing.roomId), payload)
            syncDao.updatePushedHash(pairing.personId, hash)
            null
        } catch (_: Exception) {
            "Stand konnte nicht gesendet werden"
        }
    }

    private suspend fun loadBook(personId: Long): List<BookDebt> {
        val debts = syncDao.debtsForPerson(personId)
        val syncByDebt = syncDao.debtSyncForPerson(personId).associateBy { it.debtEntryId }
        val tombstones = syncDao.tombstones(personId)
        val live = debts.map { debt ->
            val sync = syncByDebt[debt.id] ?: createDebtSync(debt.id)
            val payments = syncDao.paymentsForDebt(debt.id)
            val paySync = syncDao.paymentSyncForDebt(debt.id).associateBy { it.paymentId }
            val bookPayments = payments.map { payment ->
                val meta = paySync[payment.id] ?: createPaymentSync(payment.id)
                BookPayment(
                    syncId = meta.syncId,
                    amountCents = payment.amount.toCents(),
                    note = payment.note,
                    date = payment.date,
                    updatedAt = meta.updatedAt
                )
            }
            val removed = tombstones
                .filter { it.kind == "payment" && it.parentSyncId == sync.syncId }
                .associate { it.syncId to it.updatedAt }
            BookDebt(
                syncId = sync.syncId,
                amountCents = debt.amount.toCents(),
                currency = debt.currency,
                owedToMe = debt.isOwedToMe,
                note = debt.description,
                date = debt.date,
                dueDate = debt.dueDate,
                updatedAt = sync.updatedAt,
                cancelled = debt.status == "CANCELLED",
                deleted = false,
                payments = bookPayments,
                removedPayments = removed,
                serial = debt.syncSerial ?: debtSerial(debt.createdAt, debt.id)
            )
        }
        val liveIds = live.map { it.syncId }.toSet()
        val deleted = tombstones.filter { it.kind == "debt" && it.syncId !in liveIds }.map { stone ->
            BookDebt(
                syncId = stone.syncId,
                amountCents = 0,
                currency = "EUR",
                owedToMe = true,
                note = null,
                date = stone.updatedAt,
                dueDate = null,
                updatedAt = stone.updatedAt,
                cancelled = false,
                deleted = true,
                payments = emptyList(),
                removedPayments = emptyMap()
            )
        }
        return (live + deleted).sortedBy { it.syncId }
    }

    private suspend fun replaceLocalBook(personId: Long, remote: List<BookDebt>) {
        // Keep the previous book if any insert fails or the coroutine is cancelled.
        // Observers must never see an empty or partially replaced ledger.
        database.withTransaction {
            debtEntryDao.deleteAllForPerson(personId)
            syncDao.deleteTombstones(personId)
            for (debt in remote) {
                if (debt.deleted) {
                    rememberDebtTombstone(personId, debt)
                } else {
                    insertDebt(personId, debt)
                }
            }
        }
    }

    private suspend fun applyMerged(personId: Long, merged: List<BookDebt>) {
        for (debt in merged) {
            val existing = syncDao.debtSyncBySyncId(debt.syncId)
            if (debt.deleted) {
                if (existing != null) debtEntryDao.deleteById(existing.debtEntryId)
                rememberDebtTombstone(personId, debt)
                continue
            }
            val debtId = if (existing == null) {
                insertDebt(personId, debt)
            } else {
                updateDebt(existing, debt)
                existing.debtEntryId
            }
            syncPayments(debtId, debt)
        }
    }

    private suspend fun insertDebt(personId: Long, debt: BookDebt): Long {
        val id = debtEntryDao.insert(
            DebtEntryEntity(
                personId = personId,
                amount = debt.amountCents.fromCents(),
                currency = debt.currency,
                isOwedToMe = debt.owedToMe,
                description = debt.note,
                date = debt.date,
                dueDate = debt.dueDate,
                status = statusOf(debt),
                createdAt = debt.date,
                syncSerial = debt.serial
            )
        )
        syncDao.upsertDebtSync(DebtSyncEntity(id, debt.syncId, debt.updatedAt))
        for (payment in debt.payments) insertPayment(id, payment)
        for ((removedId, at) in debt.removedPayments) {
            syncDao.upsertTombstone(
                SyncTombstoneEntity(
                    personId = personId,
                    syncId = removedId,
                    kind = "payment",
                    parentSyncId = debt.syncId,
                    updatedAt = at
                )
            )
        }
        return id
    }

    private suspend fun updateDebt(existing: DebtSyncEntity, debt: BookDebt) {
        val row = debtEntryDao.getDebtEntryOnce(existing.debtEntryId) ?: return
        debtEntryDao.update(
            row.copy(
                amount = debt.amountCents.fromCents(),
                currency = debt.currency,
                isOwedToMe = debt.owedToMe,
                description = debt.note,
                date = debt.date,
                dueDate = debt.dueDate,
                status = statusOf(debt),
                syncSerial = debt.serial ?: row.syncSerial
            )
        )
        syncDao.touchDebt(existing.debtEntryId, debt.updatedAt)
    }

    private suspend fun syncPayments(debtId: Long, debt: BookDebt) {
        val wanted = debt.payments.associateBy { it.syncId }
        val local = syncDao.paymentSyncForDebt(debtId)
        for (row in local) {
            if (row.syncId !in wanted) paymentDao.deleteById(row.paymentId)
        }
        val have = syncDao.paymentSyncForDebt(debtId).map { it.syncId }.toSet()
        for (payment in debt.payments) {
            if (payment.syncId !in have) insertPayment(debtId, payment)
        }
        debtEntryDao.updateStatus(debtId, statusOf(debt))
        for ((removedId, at) in debt.removedPayments) {
            syncDao.upsertTombstone(
                SyncTombstoneEntity(
                    personId = syncDao.personIdOfDebt(debtId) ?: continue,
                    syncId = removedId,
                    kind = "payment",
                    parentSyncId = debt.syncId,
                    updatedAt = at
                )
            )
        }
    }

    private suspend fun insertPayment(debtId: Long, payment: BookPayment) {
        val id = paymentDao.insert(
            PaymentEntity(
                debtEntryId = debtId,
                amount = payment.amountCents.fromCents(),
                note = payment.note,
                date = payment.date
            )
        )
        syncDao.upsertPaymentSync(PaymentSyncEntity(id, payment.syncId, payment.updatedAt))
    }

    private suspend fun rememberDebtTombstone(personId: Long, debt: BookDebt) {
        syncDao.upsertTombstone(
            SyncTombstoneEntity(
                personId = personId,
                syncId = debt.syncId,
                kind = "debt",
                parentSyncId = null,
                updatedAt = debt.updatedAt
            )
        )
    }

    private suspend fun createDebtSync(debtId: Long): DebtSyncEntity {
        val created = DebtSyncEntity(debtId, UUID.randomUUID().toString(), System.currentTimeMillis())
        syncDao.upsertDebtSync(created)
        return created
    }

    private suspend fun createPaymentSync(paymentId: Long): PaymentSyncEntity {
        val created = PaymentSyncEntity(paymentId, UUID.randomUUID().toString(), System.currentTimeMillis())
        syncDao.upsertPaymentSync(created)
        return created
    }

    private companion object {
        const val WAITING_FOR_BOOK = "Noch kein Stand vom anderen Handy"
    }

    private fun statusOf(debt: BookDebt): String {
        if (debt.cancelled) return "CANCELLED"
        val paid = debt.payments.sumOf { it.amountCents }
        return when {
            paid <= 0L -> "OPEN"
            paid >= debt.amountCents -> "PAID"
            else -> "PARTIAL"
        }
    }
}

private fun Double.toCents(): Long = (this * 100.0).roundToLong()

private fun Long.fromCents(): Double = this / 100.0
