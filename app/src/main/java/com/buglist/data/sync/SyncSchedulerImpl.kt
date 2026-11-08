package com.buglist.data.sync

import com.buglist.data.local.dao.SyncDao
import com.buglist.data.local.entity.DebtSyncEntity
import com.buglist.data.local.entity.PaymentSyncEntity
import com.buglist.data.local.entity.SyncTombstoneEntity
import com.buglist.domain.repository.SyncRepository
import com.buglist.domain.repository.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSchedulerImpl @Inject constructor(
    private val syncDao: SyncDao,
    private val syncRepository: SyncRepository
) : SyncScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = ConcurrentHashMap<Long, Job>()

    override suspend fun beforeDebtDelete(debtEntryId: Long) {
        val meta = syncDao.debtSync(debtEntryId) ?: return
        val personId = syncDao.personIdOfDebt(debtEntryId) ?: return
        syncDao.upsertTombstone(
            SyncTombstoneEntity(
                personId = personId,
                syncId = meta.syncId,
                kind = "debt",
                parentSyncId = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun beforePaymentDelete(paymentId: Long) {
        val payment = syncDao.paymentSync(paymentId) ?: return
        val debtId = syncDao.debtIdOfPayment(paymentId) ?: return
        val debt = syncDao.debtSync(debtId) ?: return
        val personId = syncDao.personIdOfDebt(debtId) ?: return
        val now = System.currentTimeMillis()
        syncDao.upsertTombstone(
            SyncTombstoneEntity(
                personId = personId,
                syncId = payment.syncId,
                kind = "payment",
                parentSyncId = debt.syncId,
                updatedAt = now
            )
        )
        syncDao.touchDebt(debtId, now)
    }

    override suspend fun beforePersonWipe(personId: Long) {
        val now = System.currentTimeMillis()
        for (row in syncDao.debtSyncForPerson(personId)) {
            syncDao.upsertTombstone(
                SyncTombstoneEntity(
                    personId = personId,
                    syncId = row.syncId,
                    kind = "debt",
                    parentSyncId = null,
                    updatedAt = now
                )
            )
        }
    }

    override fun afterDebtChanged(debtId: Long, personId: Long) {
        scope.launch {
            stampDebt(debtId)
            schedule(personId)
        }
    }

    override fun afterPaymentChanged(paymentId: Long, debtId: Long, personId: Long) {
        scope.launch {
            stampDebt(debtId)
            val existing = syncDao.paymentSync(paymentId)
            val now = System.currentTimeMillis()
            if (existing == null) {
                syncDao.upsertPaymentSync(PaymentSyncEntity(paymentId, UUID.randomUUID().toString(), now))
            }
            schedule(personId)
        }
    }

    override fun afterLocalChange(personId: Long) {
        schedule(personId)
    }

    private suspend fun stampDebt(debtId: Long) {
        val now = System.currentTimeMillis()
        val existing = syncDao.debtSync(debtId)
        if (existing == null) {
            syncDao.upsertDebtSync(DebtSyncEntity(debtId, UUID.randomUUID().toString(), now))
        } else {
            syncDao.touchDebt(debtId, now)
        }
    }

    private fun schedule(personId: Long) {
        pending[personId]?.cancel()
        pending[personId] = scope.launch {
            delay(350)
            try {
                syncRepository.reconcile(personId)
            } catch (_: Exception) {
                // Mailbox failures stay quiet. The book itself is never logged.
            }
        }
    }
}
