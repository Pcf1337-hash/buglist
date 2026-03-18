package com.buglist.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.buglist.data.local.entity.PersonEntity
import com.buglist.data.local.relation.PersonWithNetBalance
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [PersonEntity].
 *
 * Reads return [Flow] for reactive UI updates.
 * Writes are `suspend fun` — must be called from a coroutine context.
 */
@Dao
interface PersonDao {

    /** Inserts a new person. Returns the auto-generated row ID. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: PersonEntity): Long

    /** Updates an existing person record. */
    @Update
    suspend fun update(person: PersonEntity)

    /** Deletes a person. All their debt entries cascade-delete automatically. */
    @Delete
    suspend fun delete(person: PersonEntity)

    /** Deletes a person by ID. */
    @Query("DELETE FROM persons WHERE id = :personId")
    suspend fun deleteById(personId: Long)

    /** Returns a person by ID, or null if not found. */
    @Query("SELECT * FROM persons WHERE id = :id")
    fun getPersonById(id: Long): Flow<PersonEntity?>

    /** Returns all persons ordered by name ascending. */
    @Query("SELECT * FROM persons ORDER BY name ASC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    /**
     * Returns all persons with their computed net balance.
     *
     * Net balance is calculated from remaining amounts (original amount minus
     * total payments) to reflect what is actually still owed.
     *
     * Positive netBalance = person owes me.
     * Negative netBalance = I owe the person.
     */
    @Transaction
    @Query("""
        SELECT
            p.*,
            COALESCE(SUM(
                CASE
                    WHEN de.isOwedToMe = 1 AND de.status NOT IN ('PAID', 'CANCELLED')
                        THEN (de.amount - COALESCE(paid.totalPaid, 0))
                    WHEN de.isOwedToMe = 0 AND de.status NOT IN ('PAID', 'CANCELLED')
                        THEN -(de.amount - COALESCE(paid.totalPaid, 0))
                    ELSE 0
                END
            ), 0) AS netBalance,
            COUNT(CASE WHEN de.status IN ('OPEN', 'PARTIAL') THEN 1 END) AS openCount
        FROM persons p
        LEFT JOIN debt_entries de ON de.personId = p.id
        LEFT JOIN (
            SELECT debtEntryId, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY debtEntryId
        ) paid ON paid.debtEntryId = de.id
        GROUP BY p.id
        ORDER BY p.name ASC
    """)
    fun getAllPersonsWithBalance(): Flow<List<PersonWithNetBalance>>

    /** Returns the total count of persons. */
    @Query("SELECT COUNT(*) FROM persons")
    fun getPersonCount(): Flow<Int>
}
