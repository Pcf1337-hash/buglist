package com.buglist.domain.repository

import com.buglist.domain.model.PairingLink
import com.buglist.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun observe(personId: Long): Flow<PairingLink?>

    /** Creates a coupling for this person and returns the code the other phone scans. */
    suspend fun startCoupling(personId: Long, bothCanEdit: Boolean): String

    suspend fun acceptCode(personId: Long, code: String): Result<PairingLink>

    suspend fun unlink(personId: Long)

    /** Pulls the other phone's snapshot and, when this phone may edit, publishes the current book. */
    suspend fun reconcile(personId: Long)
}

/**
 * Called after a local debt change. The default methods do nothing so tests can
 * construct repositories without a mailbox.
 */
interface SyncScheduler {
    suspend fun beforeDebtDelete(debtEntryId: Long) {}
    suspend fun beforePaymentDelete(paymentId: Long) {}
    suspend fun beforePersonWipe(personId: Long) {}
    fun afterDebtChanged(debtId: Long, personId: Long) {}
    fun afterPaymentChanged(paymentId: Long, debtId: Long, personId: Long) {}
    fun afterLocalChange(personId: Long) {}
}

object NoOpSyncScheduler : SyncScheduler
