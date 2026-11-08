package com.buglist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.entity.DebtSyncEntity
import com.buglist.data.local.entity.PairingEntity
import com.buglist.data.local.entity.PaymentEntity
import com.buglist.data.local.entity.PaymentSyncEntity
import com.buglist.data.local.entity.SyncTombstoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM pairings WHERE personId = :personId")
    fun observePairing(personId: Long): Flow<PairingEntity?>

    @Query("SELECT * FROM pairings WHERE personId = :personId")
    suspend fun getPairing(personId: Long): PairingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPairing(pairing: PairingEntity)

    @Query("DELETE FROM pairings WHERE personId = :personId")
    suspend fun deletePairing(personId: Long)

    @Query("UPDATE pairings SET lastPushedHash = :hash WHERE personId = :personId")
    suspend fun updatePushedHash(personId: Long, hash: String)

    @Query("SELECT * FROM debt_entries WHERE personId = :personId")
    suspend fun debtsForPerson(personId: Long): List<DebtEntryEntity>

    @Query("SELECT personId FROM debt_entries WHERE id = :debtId")
    suspend fun personIdOfDebt(debtId: Long): Long?

    @Query("SELECT * FROM payments WHERE debtEntryId = :debtId")
    suspend fun paymentsForDebt(debtId: Long): List<PaymentEntity>

    @Query("SELECT debtEntryId FROM payments WHERE id = :paymentId")
    suspend fun debtIdOfPayment(paymentId: Long): Long?

    @Query("SELECT * FROM debt_sync WHERE debtEntryId = :debtId")
    suspend fun debtSync(debtId: Long): DebtSyncEntity?

    @Query("SELECT * FROM debt_sync WHERE syncId = :syncId")
    suspend fun debtSyncBySyncId(syncId: String): DebtSyncEntity?

    @Query("SELECT * FROM debt_sync WHERE debtEntryId IN (SELECT id FROM debt_entries WHERE personId = :personId)")
    suspend fun debtSyncForPerson(personId: Long): List<DebtSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDebtSync(row: DebtSyncEntity)

    @Query("UPDATE debt_sync SET updatedAt = :updatedAt WHERE debtEntryId = :debtId")
    suspend fun touchDebt(debtId: Long, updatedAt: Long)

    @Query("SELECT * FROM payment_sync WHERE paymentId = :paymentId")
    suspend fun paymentSync(paymentId: Long): PaymentSyncEntity?

    @Query("SELECT * FROM payment_sync WHERE syncId = :syncId")
    suspend fun paymentSyncBySyncId(syncId: String): PaymentSyncEntity?

    @Query(
        "SELECT * FROM payment_sync WHERE paymentId IN (SELECT id FROM payments WHERE debtEntryId = :debtId)"
    )
    suspend fun paymentSyncForDebt(debtId: Long): List<PaymentSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaymentSync(row: PaymentSyncEntity)

    @Query("SELECT * FROM sync_tombstones WHERE personId = :personId")
    suspend fun tombstones(personId: Long): List<SyncTombstoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTombstone(row: SyncTombstoneEntity)

    @Query("DELETE FROM sync_tombstones WHERE personId = :personId")
    suspend fun deleteTombstones(personId: Long)
}
