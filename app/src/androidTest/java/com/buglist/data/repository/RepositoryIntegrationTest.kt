package com.buglist.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buglist.data.local.AppDatabase
import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Payment
import com.buglist.domain.model.Person
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

/**
 * Integration tests for all three repository implementations.
 *
 * Uses an in-memory SQLCipher database. Verifies:
 * - PersonRepository: CRUD, balance computation
 * - DebtRepository: CRUD, status filtering, net balance
 * - PaymentRepository: atomic insert + status update (L-035)
 * - Cascade delete: person → debt entries → payments
 * - Balance based on remaining, not original amount
 */
@RunWith(AndroidJUnit4::class)
class RepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var personRepo: PersonRepositoryImpl
    private lateinit var debtRepo: DebtRepositoryImpl
    private lateinit var paymentRepo: PaymentRepositoryImpl

    @Before
    fun setup() {
        System.loadLibrary("sqlcipher")

        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .allowMainThreadQueries()
            .build()

        personRepo = PersonRepositoryImpl(db.personDao())
        debtRepo = DebtRepositoryImpl(db.debtEntryDao())
        paymentRepo = PaymentRepositoryImpl(db.paymentDao(), db.debtEntryDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- PersonRepository Tests ---

    @Test
    fun addAndRetrievePerson() = runTest {
        val id = personRepo.addPerson(Person(name = "Alice", avatarColor = 0xFF0000.toInt()))
        assertTrue(id > 0)

        val person = personRepo.getPersonById(id).first()
        assertEquals("Alice", person?.name)
    }

    @Test
    fun updatePerson() = runTest {
        val id = personRepo.addPerson(Person(name = "Alice"))
        personRepo.updatePerson(Person(id = id, name = "Alice Updated"))

        val person = personRepo.getPersonById(id).first()
        assertEquals("Alice Updated", person?.name)
    }

    @Test
    fun deletePersonCascadesDebtEntries() = runTest {
        val personId = personRepo.addPerson(Person(name = "Bob"))
        val debtId = debtRepo.addDebtEntry(
            DebtEntry(personId = personId, amount = 100.0, isOwedToMe = true,
                date = System.currentTimeMillis())
        )
        assertTrue(debtId > 0)

        personRepo.deletePersonById(personId)

        val debts = debtRepo.getDebtEntriesWithPaymentsForPerson(personId).first()
        assertTrue(debts.isEmpty())
    }

    @Test
    fun personCountUpdatesReactively() = runTest {
        assertEquals(0, personRepo.getPersonCount().first())
        personRepo.addPerson(Person(name = "Alice"))
        assertEquals(1, personRepo.getPersonCount().first())
        personRepo.addPerson(Person(name = "Bob"))
        assertEquals(2, personRepo.getPersonCount().first())
    }

    // --- DebtRepository Tests ---

    @Test
    fun netBalanceIsPositiveWhenPersonOwesMe() = runTest {
        val personId = personRepo.addPerson(Person(name = "Charlie"))
        debtRepo.addDebtEntry(
            DebtEntry(personId = personId, amount = 200.0, isOwedToMe = true,
                date = System.currentTimeMillis())
        )

        val persons = personRepo.getAllPersonsWithBalance().first()
        val charlie = persons.first { it.person.name == "Charlie" }
        assertEquals(200.0, charlie.netBalance, 0.001)
    }

    @Test
    fun netBalanceIsNegativeWhenIOwe() = runTest {
        val personId = personRepo.addPerson(Person(name = "Dana"))
        debtRepo.addDebtEntry(
            DebtEntry(personId = personId, amount = 150.0, isOwedToMe = false,
                date = System.currentTimeMillis())
        )

        val persons = personRepo.getAllPersonsWithBalance().first()
        val dana = persons.first { it.person.name == "Dana" }
        assertEquals(-150.0, dana.netBalance, 0.001)
    }

    @Test
    fun netBalanceExcludesPaidDebts() = runTest {
        val personId = personRepo.addPerson(Person(name = "Eve"))
        val debtId = debtRepo.addDebtEntry(
            DebtEntry(personId = personId, amount = 100.0, isOwedToMe = true,
                date = System.currentTimeMillis())
        )
        debtRepo.updateDebtStatus(debtId, DebtStatus.PAID)

        val persons = personRepo.getAllPersonsWithBalance().first()
        val eve = persons.first { it.person.name == "Eve" }
        assertEquals(0.0, eve.netBalance, 0.001)
    }

    @Test
    fun netBalanceBasedOnRemainingNotOriginal() = runTest {
        val personId = personRepo.addPerson(Person(name = "Frank"))
        val debtId = debtRepo.addDebtEntry(
            DebtEntry(personId = personId, amount = 100.0, isOwedToMe = true,
                date = System.currentTimeMillis())
        )
        // Pay 60 — remaining should be 40
        paymentRepo.insertPaymentAndUpdateStatus(
            Payment(debtEntryId = debtId, amount = 60.0),
            DebtStatus.PARTIAL.name
        )

        val persons = personRepo.getAllPersonsWithBalance().first()
        val frank = persons.first { it.person.name == "Frank" }
        assertEquals(40.0, frank.netBalance, 0.001)
    }

    // --- PaymentRepository Tests ---

    @Test
    fun paymentInsertAndStatusUpdateAreAtomic() = runTest {
        val personId = personRepo.addPerson(Person(name = "Grace"))
        val debtId = debtRepo.addDebtEntry(
            DebtEntry(personId = personId, amount = 100.0, isOwedToMe = true,
                date = System.currentTimeMillis())
        )

        paymentRepo.insertPaymentAndUpdateStatus(
            Payment(debtEntryId = debtId, amount = 50.0),
            DebtStatus.PARTIAL.name
        )

        val dwp = debtRepo.getDebtEntryWithPayments(debtId).first()
        assertEquals(DebtStatus.PARTIAL, dwp?.entry?.status)
        assertEquals(50.0, dwp?.totalPaid ?: 0.0, 0.001)
        assertEquals(50.0, dwp?.remaining ?: 0.0, 0.001)
    }

    @Test
    fun fullPaymentSetsStatusToPaid() = runTest {
        val personId = personRepo.addPerson(Person(name = "Henry"))
        val debtId = debtRepo.addDebtEntry(
            DebtEntry(personId = personId, amount = 75.0, isOwedToMe = true,
                date = System.currentTimeMillis())
        )

        paymentRepo.insertPaymentAndUpdateStatus(
            Payment(debtEntryId = debtId, amount = 75.0),
            DebtStatus.PAID.name
        )

        val dwp = debtRepo.getDebtEntryWithPayments(debtId).first()
        assertEquals(DebtStatus.PAID, dwp?.entry?.status)
        assertEquals(0.0, dwp?.remaining ?: 1.0, 0.001)
    }

    @Test
    fun multiplePartialPaymentsAccumulate() = runTest {
        val personId = personRepo.addPerson(Person(name = "Iris"))
        val debtId = debtRepo.addDebtEntry(
            DebtEntry(personId = personId, amount = 100.0, isOwedToMe = true,
                date = System.currentTimeMillis())
        )

        paymentRepo.insertPaymentAndUpdateStatus(
            Payment(debtEntryId = debtId, amount = 30.0), DebtStatus.PARTIAL.name
        )
        paymentRepo.insertPaymentAndUpdateStatus(
            Payment(debtEntryId = debtId, amount = 30.0), DebtStatus.PARTIAL.name
        )
        paymentRepo.insertPaymentAndUpdateStatus(
            Payment(debtEntryId = debtId, amount = 40.0), DebtStatus.PAID.name
        )

        val dwp = debtRepo.getDebtEntryWithPayments(debtId).first()
        assertEquals(DebtStatus.PAID, dwp?.entry?.status)
        assertEquals(100.0, dwp?.totalPaid ?: 0.0, 0.001)
        assertEquals(0.0, dwp?.remaining ?: 1.0, 0.001)
        assertEquals(3, dwp?.payments?.size)
    }

    @Test
    fun totalNetBalanceAcrossAllPersons() = runTest {
        val p1 = personRepo.addPerson(Person(name = "Jack"))
        val p2 = personRepo.addPerson(Person(name = "Kate"))

        debtRepo.addDebtEntry(DebtEntry(personId = p1, amount = 200.0, isOwedToMe = true,
            date = System.currentTimeMillis()))
        debtRepo.addDebtEntry(DebtEntry(personId = p2, amount = 80.0, isOwedToMe = false,
            date = System.currentTimeMillis()))

        val totalBalance = debtRepo.getTotalNetBalance().first()
        assertEquals(120.0, totalBalance, 0.001)  // 200 - 80 = 120
    }
}
