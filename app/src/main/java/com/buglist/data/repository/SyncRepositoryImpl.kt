package com.buglist.data.repository

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
import com.buglist.data.sync.WireBook
import com.buglist.data.sync.toLocalBook
import com.buglist.data.sync.toWire
import com.buglist.domain.model.PairingLink
import com.buglist.domain.model.Result
import com.buglist.domain.model.SyncRole
import com.buglist.domain.repository.SyncRepository
import com.buglist.domain.sync.BookDebt
import com.buglist.domain.sync.BookPayment
import com.buglist.domain.sync.mergeBooks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val changeSignal: ChangeSignal
) : SyncRepository {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val locks = ConcurrentHashMap<Long, Mutex>()

    override fun observe(personId: Long): Flow<PairingLink?> =
        syncDao.observePairing(personId).map { entity ->
            entity?.let { PairingLink(SyncRole.fromStored(it.role)) }
        }

    override suspend fun startCoupling(personId: Long, bothCanEdit: Boolean): String {
        val existing = syncDao.getPairing(personId)
        if (existing != null) {
            return PairingCode.encode(existing.roomId, existing.keyMaterial, existing.role == "BOTH")
        }
        val pairing = PairingEntity(
            personId = personId,
            role = if (bothCanEdit) "BOTH" else "HOST",
            roomId = PairingCode.randomRoomId(),
            keyMaterial = PairingCode.randomKey(),
            deviceId = PairingCode.randomDeviceId(),
            lastPushedHash = "",
            linkedAt = System.currentTimeMillis()
        )
        syncDao.upsertPairing(pairing)
        reconcile(personId)
        return PairingCode.encode(pairing.roomId, pairing.keyMaterial, bothCanEdit)
    }

    override suspend fun acceptCode(personId: Long, code: String): Result<PairingLink> {
        val offer = PairingCode.decode(code)
            ?: return Result.Error("Das ist kein BugList-Kopplungscode")
        val role = if (offer.scannerIsGuest) SyncRole.GUEST else SyncRole.BOTH
        syncDao.upsertPairing(
            PairingEntity(
                personId = personId,
                role = role.name,
                roomId = offer.roomId,
                keyMaterial = offer.keyBase64Url,
                deviceId = PairingCode.randomDeviceId(),
                lastPushedHash = "",
                linkedAt = System.currentTimeMillis()
            )
        )
        reconcile(personId)
        return Result.Success(PairingLink(role))
    }

    override suspend fun unlink(personId: Long) {
        syncDao.deletePairing(personId)
    }

    override suspend fun reconcile(personId: Long) {
        val mutex = locks.getOrPut(personId) { Mutex() }
        mutex.withLock {
            val pairing = syncDao.getPairing(personId) ?: return
            val role = SyncRole.fromStored(pairing.role)
            if (role != SyncRole.HOST) {
                pull(pairing, role)
            }
            if (role != SyncRole.GUEST) {
                push(syncDao.getPairing(personId) ?: return)
            }
        }
    }

    private suspend fun pull(pairing: PairingEntity, role: SyncRole) {
        val payload = try {
            mailbox.latestPayload(PairingCode.topicFor(pairing.roomId))
        } catch (_: Exception) {
            return
        } ?: return
        val plain = SnapshotCrypto.decrypt(payload, pairing.keyMaterial) ?: return
        val wire = try {
            json.decodeFromString<WireBook>(plain.decodeToString())
        } catch (_: Exception) {
            return
        }
        if (wire.from == pairing.deviceId) return
        val remote = wire.debts.map { it.toLocalBook() }
        if (role == SyncRole.GUEST) {
            replaceLocalBook(pairing.personId, remote)
        } else {
            val local = loadBook(pairing.personId)
            applyMerged(pairing.personId, mergeBooks(local, remote))
        }
        changeSignal.bump()
    }

    private suspend fun push(pairing: PairingEntity) {
        val book = loadBook(pairing.personId)
        val wire = WireBook(
            from = pairing.deviceId,
            debts = book.map { it.toWire() }
        )
        val bytes = json.encodeToString(wire).encodeToByteArray()
        val hash = SnapshotCrypto.sha256(bytes)
        if (hash == pairing.lastPushedHash) return
        val payload = SnapshotCrypto.encrypt(bytes, pairing.keyMaterial)
        try {
            mailbox.publish(PairingCode.topicFor(pairing.roomId), payload)
            syncDao.updatePushedHash(pairing.personId, hash)
        } catch (_: Exception) {
            // The next open retries. Nothing about the book is written to a log.
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
                removedPayments = removed
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
                createdAt = debt.date
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
                status = statusOf(debt)
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
