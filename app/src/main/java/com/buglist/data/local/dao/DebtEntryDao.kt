package com.buglist.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.relation.DebtEntryWithPaymentsRelation
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [DebtEntryEntity].
 *
 * Reads return [Flow] for reactive UI updates.
 * Writes are `suspend fun` — must be called from a coroutine context.
 */
@Dao
interface DebtEntryDao {

    /** Inserts a new debt entry. Returns the auto-generated row ID. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(debtEntry: DebtEntryEntity): Long

    /** Updates an existing debt entry (e.g., to change status). */
    @Update
    suspend fun update(debtEntry: DebtEntryEntity)

    /** Deletes a debt entry. All associated payments cascade-delete automatically. */
    @Delete
    suspend fun delete(debtEntry: DebtEntryEntity)

    /** Deletes a debt entry by ID. */
    @Query("DELETE FROM debt_entries WHERE id = :debtEntryId")
    suspend fun deleteById(debtEntryId: Long)

    /**
     * Deletes ALL debt entries for a person (CASCADE deletes all their payments).
     * Used by [ImportDebtListUseCase] to replace the full list during a sync import.
     */
    @Query("DELETE FROM debt_entries WHERE personId = :personId")
    suspend fun deleteAllForPerson(personId: Long)

    /**
     * Reactive count of all debt entries.
     *
     * Used as an InvalidationTracker-compatible trigger: emits on every insert,
     * update, or delete of [DebtEntryEntity] so that ViewModels using SQLCipher
     * @Transaction queries can force pipeline re-evaluation. (L-040)
     */
    @Query("SELECT COUNT(*) FROM debt_entries")
    fun getDebtEntryCount(): Flow<Int>

    /**
     * Updates only the status column of a debt entry.
     * Used by the payment repository after inserting a payment to keep status consistent.
     */
    @Query("UPDATE debt_entries SET status = :status WHERE id = :debtEntryId")
    suspend fun updateStatus(debtEntryId: Long, status: String)

    /** Returns a single debt entry by ID. */
    @Query("SELECT * FROM debt_entries WHERE id = :id")
    fun getDebtEntryById(id: Long): Flow<DebtEntryEntity?>

    /** Returns all debt entries for a person, ordered by date descending. */
    @Query("""
        SELECT * FROM debt_entries
        WHERE personId = :personId
        ORDER BY date DESC
    """)
    fun getDebtEntriesForPerson(personId: Long): Flow<List<DebtEntryEntity>>

    /** Returns debt entries for a person filtered by status. */
    @Query("""
        SELECT * FROM debt_entries
        WHERE personId = :personId
          AND status IN (:statuses)
        ORDER BY date DESC
    """)
    fun getDebtEntriesForPersonByStatus(
        personId: Long,
        statuses: List<String>
    ): Flow<List<DebtEntryEntity>>

    /**
     * Returns all debt entries with their associated payments.
     * Used for the person detail screen which shows payment progress bars.
     */
    @Transaction
    @Query("""
        SELECT * FROM debt_entries
        WHERE personId = :personId
        ORDER BY date DESC
    """)
    fun getDebtEntriesWithPaymentsForPerson(
        personId: Long
    ): Flow<List<DebtEntryWithPaymentsRelation>>

    /**
     * Returns a single debt entry with all its payments.
     * Used when recording a partial payment to calculate remaining amount.
     */
    @Transaction
    @Query("SELECT * FROM debt_entries WHERE id = :debtEntryId")
    fun getDebtEntryWithPayments(
        debtEntryId: Long
    ): Flow<DebtEntryWithPaymentsRelation?>

    /**
     * Returns debt entries for the dashboard recent-activity feed.
     * Limited to [limit] most recently modified entries across all persons.
     */
    @Transaction
    @Query("""
        SELECT * FROM debt_entries
        WHERE status IN (:statuses)
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    fun getRecentDebtEntries(
        statuses: List<String>,
        limit: Int = 20
    ): Flow<List<DebtEntryWithPaymentsRelation>>

    /**
     * Returns monthly debt totals for the last 6 months for the statistics chart.
     * Returns (yearMonth, totalOwedToMe, totalIOwe) tuples.
     */
    @Query("""
        SELECT
            strftime('%Y-%m', date / 1000, 'unixepoch') AS yearMonth,
            SUM(CASE WHEN isOwedToMe = 1 THEN amount ELSE 0 END) AS totalOwedToMe,
            SUM(CASE WHEN isOwedToMe = 0 THEN amount ELSE 0 END) AS totalIOwe
        FROM debt_entries
        WHERE date >= :fromTimestamp
          AND status NOT IN ('CANCELLED')
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
    """)
    fun getMonthlyTotals(fromTimestamp: Long): Flow<List<MonthlyTotalRow>>

    /**
     * Returns all open (OPEN or PARTIAL) debt entries for a person, sorted by date ascending.
     *
     * Used by SettleDebtsUseCase to build the FIFO settlement queue — oldest entry first.
     *
     * @param personId   The person whose debts to load.
     * @param isOwedToMe 1 = entries where the person owes the user; 0 = entries where the user owes.
     */
    @Transaction
    @Query("""
        SELECT * FROM debt_entries
        WHERE personId = :personId
          AND isOwedToMe = :isOwedToMe
          AND status IN ('OPEN', 'PARTIAL')
        ORDER BY date ASC
    """)
    suspend fun getOpenDebtsForPersonOrderedByDate(
        personId: Long,
        isOwedToMe: Boolean
    ): List<DebtEntryWithPaymentsRelation>

    /**
     * Searches debt entries by description text across all persons.
     */
    @Transaction
    @Query("""
        SELECT * FROM debt_entries
        WHERE description LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    fun searchByDescription(query: String): Flow<List<DebtEntryWithPaymentsRelation>>

    /**
     * Returns the count of debt entries per status for the statistics screen.
     */
    @Query("""
        SELECT status, COUNT(*) AS count
        FROM debt_entries
        WHERE status != 'CANCELLED'
        GROUP BY status
    """)
    fun getStatusCounts(): Flow<List<StatusCountRow>>

    /**
     * Returns the total amount of fully paid entries (PAID status).
     * Split by direction: owedToMe = amount received back, iOwe = amount paid out.
     */
    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN isOwedToMe = 1 THEN amount ELSE 0 END), 0) AS totalPaidOwedToMe,
            COALESCE(SUM(CASE WHEN isOwedToMe = 0 THEN amount ELSE 0 END), 0) AS totalPaidIOwe
        FROM debt_entries
        WHERE status = 'PAID'
    """)
    fun getPaidTotals(): Flow<PaidTotalsRow>

    /**
     * Returns the last [limit] debt entries (newest first) for the statistics activity timeline.
     */
    @Transaction
    @Query("""
        SELECT * FROM debt_entries
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    fun getLatestEntries(limit: Int = 7): Flow<List<DebtEntryWithPaymentsRelation>>

    /**
     * Returns the sum of open amounts per direction for the statistics header cards.
     * Only OPEN and PARTIAL entries are counted (remaining-based).
     */
    @Query("""
        SELECT
            COALESCE(SUM(
                CASE WHEN de.isOwedToMe = 1 AND de.status IN ('OPEN', 'PARTIAL')
                    THEN (de.amount - COALESCE(paid.totalPaid, 0))
                    ELSE 0 END
            ), 0) AS openOwedToMe,
            COALESCE(SUM(
                CASE WHEN de.isOwedToMe = 0 AND de.status IN ('OPEN', 'PARTIAL')
                    THEN (de.amount - COALESCE(paid.totalPaid, 0))
                    ELSE 0 END
            ), 0) AS openIOwe,
            COUNT(CASE WHEN de.status IN ('OPEN', 'PARTIAL') THEN 1 END) AS totalOpenCount
        FROM debt_entries de
        LEFT JOIN (
            SELECT debtEntryId, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY debtEntryId
        ) paid ON paid.debtEntryId = de.id
    """)
    fun getOpenTotals(): Flow<OpenTotalsRow>

    /**
     * Returns the sum of open debt amounts that are older than [thresholdMs] ms (e.g. 60 days).
     * Used for the At-Risk bento card in statistics.
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN de.isOwedToMe = 1
                THEN (de.amount - COALESCE(paid.totalPaid, 0))
                ELSE 0 END
        ), 0) AS atRiskAmount
        FROM debt_entries de
        LEFT JOIN (
            SELECT debtEntryId, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY debtEntryId
        ) paid ON paid.debtEntryId = de.id
        WHERE de.status IN ('OPEN', 'PARTIAL')
          AND de.date <= :thresholdMs
    """)
    fun getAtRiskAmount(thresholdMs: Long): Flow<AtRiskRow>

    /**
     * Returns the average duration (in days) of currently open/partial debt entries.
     * Computed as (now - date) / 86400000.
     * Used for the Ø-Dauer bento card in statistics.
     */
    @Query("""
        SELECT COALESCE(AVG((:nowMs - date) / 86400000.0), 0) AS avgDays
        FROM debt_entries
        WHERE status IN ('OPEN', 'PARTIAL')
    """)
    fun getAvgDebtDurationDays(nowMs: Long): Flow<AvgDurationRow>

    /**
     * Returns paid count vs total count for computing the overall repayment rate.
     * Excludes CANCELLED entries from total.
     */
    @Query("""
        SELECT
            COUNT(CASE WHEN status = 'PAID' THEN 1 END) AS paidCount,
            COUNT(CASE WHEN status != 'CANCELLED' THEN 1 END) AS totalCount
        FROM debt_entries
    """)
    fun getRepaymentRate(): Flow<RepaymentRateRow>

    /**
     * Returns per-person paid/total counts for computing individual reliability scores.
     * Excludes CANCELLED entries.
     */
    @Query("""
        SELECT
            personId,
            COUNT(CASE WHEN status = 'PAID' THEN 1 END) AS paidCount,
            COUNT(CASE WHEN status != 'CANCELLED' THEN 1 END) AS totalCount
        FROM debt_entries
        GROUP BY personId
    """)
    fun getPersonReliabilityScores(): Flow<List<PersonReliabilityRow>>

    /**
     * Returns daily activity counts (payments + new debts) for the heatmap.
     * [fromMs] = start of 91-day window (13 weeks back from today).
     */
    @Query("""
        SELECT
            (date / 86400000) * 86400000 AS dayEpochMs,
            0 AS paymentCount,
            COUNT(*) AS newDebtCount
        FROM debt_entries
        WHERE date >= :fromMs
        GROUP BY dayEpochMs
    """)
    fun getNewDebtActivityByDay(fromMs: Long): Flow<List<DayActivityRow>>

    /**
     * Returns daily payment activity for the heatmap.
     * [fromMs] = start of 91-day window.
     */
    @Query("""
        SELECT
            (date / 86400000) * 86400000 AS dayEpochMs,
            COUNT(*) AS paymentCount,
            0 AS newDebtCount
        FROM payments
        WHERE date >= :fromMs
        GROUP BY dayEpochMs
    """)
    fun getPaymentActivityByDay(fromMs: Long): Flow<List<DayActivityRow>>

    /**
     * Returns the global net balance across all persons:
     * positive = net owed to me, negative = net I owe.
     * Based on remaining amounts (amount - totalPaid) for non-terminal entries.
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN de.isOwedToMe = 1 AND de.status NOT IN ('PAID', 'CANCELLED')
                    THEN (de.amount - COALESCE(paid.totalPaid, 0))
                WHEN de.isOwedToMe = 0 AND de.status NOT IN ('PAID', 'CANCELLED')
                    THEN -(de.amount - COALESCE(paid.totalPaid, 0))
                ELSE 0
            END
        ), 0) AS totalBalance
        FROM debt_entries de
        LEFT JOIN (
            SELECT debtEntryId, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY debtEntryId
        ) paid ON paid.debtEntryId = de.id
    """)
    fun getTotalNetBalance(): Flow<Double>
}

/** Projection for the monthly totals chart query. */
data class MonthlyTotalRow(
    val yearMonth: String,
    val totalOwedToMe: Double,
    val totalIOwe: Double
)

/** Projection for status count query. */
data class StatusCountRow(
    val status: String,
    val count: Int
)

/** Projection for paid totals query. */
data class PaidTotalsRow(
    val totalPaidOwedToMe: Double,
    val totalPaidIOwe: Double
)

/** Projection for open totals summary query. */
data class OpenTotalsRow(
    val openOwedToMe: Double,
    val openIOwe: Double,
    val totalOpenCount: Int
)

/** Projection for at-risk amount query (debts open > N days). */
data class AtRiskRow(
    val atRiskAmount: Double
)

/** Projection for average debt duration query. */
data class AvgDurationRow(
    val avgDays: Double
)

/** Projection for repayment rate query. */
data class RepaymentRateRow(
    val paidCount: Int,
    val totalCount: Int
)

/** Projection for per-person reliability score. */
data class PersonReliabilityRow(
    val personId: Long,
    val paidCount: Int,
    val totalCount: Int
)

/** Projection for daily activity heatmap. */
data class DayActivityRow(
    val dayEpochMs: Long,
    val paymentCount: Int,
    val newDebtCount: Int
)
