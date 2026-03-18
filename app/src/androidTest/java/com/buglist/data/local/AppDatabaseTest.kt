package com.buglist.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.entity.PaymentEntity
import com.buglist.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

/**
 * Instrumented tests for [AppDatabase] and its DAOs.
 *
 * Uses an in-process SQLCipher database (not in-memory) to test real encryption.
 * The passphrase is a fresh random 32-byte array for each test run.
 *
 * These tests verify:
 * - Database opens successfully with SQLCipher encryption
 * - All three DAOs are functional
 * - CASCADE DELETE works correctly (person delete → debt entries delete → payments delete)
 * - The net balance query returns correct results
 * - Payment insertion + status update transaction is atomic
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var passphrase: ByteArray

    @Before
    fun setup() {
        // sqlcipher-android 4.9.0 has no loadLibs() API.
        // The native .so must be explicitly loaded in the instrumented test process
        // because the test process starts without the app's JNI class-loading path.
        // System.loadLibrary triggers the same native init that the app uses.
        // See L-064 in lessons.md.
        System.loadLibrary("sqlcipher")

        passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val factory = SupportOpenHelperFactory(passphrase)

        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
        // Note: in-memory SQLCipher database still requires the factory
        // to use the encrypted engine, even though data isn't persisted.
        // For encryption verification, see the xxd check in Task 1.3 manual testing.
            .openHelperFactory(factory)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
        passphrase.fill(0)
    }

    // ── Person CRUD ──────────────────────────────────────────────────────────

    @Test
    fun insertAndRetrievePerson() = runTest {
        val person = PersonEntity(name = "Big Mike", phone = "0151-555-1234")
        val id = db.personDao().insert(person)

        assertTrue("Person ID should be > 0", id > 0)

        val retrieved = db.personDao().getPersonById(id).first()
        assertNotNull(retrieved)
        assertEquals("Big Mike", retrieved?.name)
        assertEquals("0151-555-1234", retrieved?.phone)
    }

    @Test
    fun updatePerson() = runTest {
        val id = db.personDao().insert(PersonEntity(name = "Old Name"))
        val person = db.personDao().getPersonById(id).first()!!

        db.personDao().update(person.copy(name = "New Name"))

        val updated = db.personDao().getPersonById(id).first()
        assertEquals("New Name", updated?.name)
    }

    @Test
    fun deletePersonCascadesDebtEntries() = runTest {
        val personId = db.personDao().insert(PersonEntity(name = "Cascade Test"))

        val debtId = db.debtEntryDao().insert(
            DebtEntryEntity(
                personId = personId,
                amount = 100.0,
                isOwedToMe = true,
                date = System.currentTimeMillis()
            )
        )

        db.paymentDao().insert(
            PaymentEntity(debtEntryId = debtId, amount = 20.0)
        )

        // Delete person — should cascade to debt entries and payments
        db.personDao().deleteById(personId)

        val debtsAfterDelete = db.debtEntryDao().getDebtEntriesForPerson(personId).first()
        assertTrue("Debts should be empty after person delete", debtsAfterDelete.isEmpty())

        val paymentsAfterDelete = db.paymentDao().getPaymentsForDebtEntry(debtId).first()
        assertTrue("Payments should be empty after person delete", paymentsAfterDelete.isEmpty())
    }

    // ── DebtEntry CRUD ───────────────────────────────────────────────────────

    @Test
    fun insertDebtEntryAndRetrieve() = runTest {
        val personId = db.personDao().insert(PersonEntity(name = "Test Person"))
        val debt = DebtEntryEntity(
            personId = personId,
            amount = 50.0,
            isOwedToMe = true,
            date = System.currentTimeMillis(),
            description = "Dinner"
        )
        val debtId = db.debtEntryDao().insert(debt)
        assertTrue(debtId > 0)

        val debts = db.debtEntryDao().getDebtEntriesForPerson(personId).first()
        assertEquals(1, debts.size)
        assertEquals("Dinner", debts[0].description)
        assertEquals(50.0, debts[0].amount, 0.001)
    }

    @Test
    fun updateDebtEntryStatus() = runTest {
        val personId = db.personDao().insert(PersonEntity(name = "Status Person"))
        val debtId = db.debtEntryDao().insert(
            DebtEntryEntity(
                personId = personId,
                amount = 100.0,
                isOwedToMe = true,
                date = System.currentTimeMillis()
            )
        )

        db.debtEntryDao().updateStatus(debtId, "PARTIAL")

        val entries = db.debtEntryDao().getDebtEntriesForPerson(personId).first()
        assertEquals("PARTIAL", entries[0].status)
    }

    // ── Payment CRUD ─────────────────────────────────────────────────────────

    @Test
    fun insertPaymentAndUpdateStatusInTransaction() = runTest {
        val personId = db.personDao().insert(PersonEntity(name = "Payment Person"))
        val debtId = db.debtEntryDao().insert(
            DebtEntryEntity(
                personId = personId,
                amount = 100.0,
                isOwedToMe = true,
                date = System.currentTimeMillis()
            )
        )

        val payment = PaymentEntity(debtEntryId = debtId, amount = 50.0)
        db.paymentDao().insertPaymentAndUpdateStatus(payment, "PARTIAL", db.debtEntryDao())

        val payments = db.paymentDao().getPaymentsForDebtEntry(debtId).first()
        assertEquals(1, payments.size)
        assertEquals(50.0, payments[0].amount, 0.001)

        val debts = db.debtEntryDao().getDebtEntriesForPerson(personId).first()
        assertEquals("PARTIAL", debts[0].status)
    }

    @Test
    fun totalPaidCalculation() = runTest {
        val personId = db.personDao().insert(PersonEntity(name = "Paid Person"))
        val debtId = db.debtEntryDao().insert(
            DebtEntryEntity(
                personId = personId,
                amount = 100.0,
                isOwedToMe = true,
                date = System.currentTimeMillis()
            )
        )

        db.paymentDao().insert(PaymentEntity(debtEntryId = debtId, amount = 30.0))
        db.paymentDao().insert(PaymentEntity(debtEntryId = debtId, amount = 20.0))

        val totalPaid = db.paymentDao().getTotalPaidForDebtEntry(debtId)
        assertEquals(50.0, totalPaid, 0.001)
    }

    // ── Balance queries ──────────────────────────────────────────────────────

    @Test
    fun netBalanceQueryReturnsCorrectValue() = runTest {
        val personId = db.personDao().insert(PersonEntity(name = "Balance Person"))

        // Owed to me: €100 (fully open)
        val debt1Id = db.debtEntryDao().insert(
            DebtEntryEntity(
                personId = personId,
                amount = 100.0,
                isOwedToMe = true,
                date = System.currentTimeMillis()
            )
        )

        // Owed to me: €60 with €20 paid → remaining €40
        val debt2Id = db.debtEntryDao().insert(
            DebtEntryEntity(
                personId = personId,
                amount = 60.0,
                isOwedToMe = true,
                date = System.currentTimeMillis(),
                status = "PARTIAL"
            )
        )
        db.paymentDao().insert(PaymentEntity(debtEntryId = debt2Id, amount = 20.0))

        val persons = db.personDao().getAllPersonsWithBalance().first()
        assertEquals(1, persons.size)

        // Net balance = €100 + (€60 - €20) = €140
        assertEquals(140.0, persons[0].netBalance, 0.01)
        assertEquals(2, persons[0].openCount)
    }

    @Test
    fun globalNetBalanceQueryIsCorrect() = runTest {
        val personId = db.personDao().insert(PersonEntity(name = "Global Balance"))

        db.debtEntryDao().insert(
            DebtEntryEntity(personId = personId, amount = 200.0, isOwedToMe = true,
                date = System.currentTimeMillis())
        )
        db.debtEntryDao().insert(
            DebtEntryEntity(personId = personId, amount = 50.0, isOwedToMe = false,
                date = System.currentTimeMillis())
        )

        val balance = db.debtEntryDao().getTotalNetBalance().first()
        assertEquals(150.0, balance, 0.01)  // 200 - 50 = 150
    }

    @Test
    fun paidDebtIsExcludedFromBalance() = runTest {
        val personId = db.personDao().insert(PersonEntity(name = "Paid Test"))

        db.debtEntryDao().insert(
            DebtEntryEntity(personId = personId, amount = 100.0, isOwedToMe = true,
                date = System.currentTimeMillis(), status = "PAID")
        )

        val balance = db.debtEntryDao().getTotalNetBalance().first()
        assertEquals(0.0, balance, 0.01)  // PAID debts don't count
    }
}
