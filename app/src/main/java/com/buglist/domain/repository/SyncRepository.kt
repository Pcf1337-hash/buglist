package com.buglist.domain.repository

import com.buglist.domain.model.PairingLink
import com.buglist.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun observe(personId: Long): Flow<PairingLink?>

    /** Creates a coupling for this person and returns the code the other phone scans. */
    suspend fun startCoupling(personId: Long, bothCanEdit: Boolean): String

    suspend fun acceptCode(personId: Long, code: String): Result<PairingLink>

    /** After the code is exchanged, each phone picks host or view-only. */
    suspend fun claimRole(personId: Long, host: Boolean): Result<Unit>

    suspend fun unlink(personId: Long)

    /**
     * Host publishes the current book. Guest replaces the local book with it.
     * The error text names the failure and never includes amounts or names.
     */
    suspend fun reconcile(personId: Long): Result<Unit>
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
