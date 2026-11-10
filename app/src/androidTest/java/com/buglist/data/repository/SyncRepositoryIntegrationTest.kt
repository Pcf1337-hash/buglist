package com.buglist.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buglist.data.local.AppDatabase
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.entity.PairingEntity
import com.buglist.data.local.entity.PaymentEntity
import com.buglist.data.local.entity.PersonEntity
import com.buglist.data.sync.ChangeSignal
import com.buglist.data.sync.NtfyMailbox
import com.buglist.data.sync.PairingCode
import com.buglist.data.sync.SnapshotCrypto
import com.buglist.data.sync.WireBook
import com.buglist.data.sync.WireDebt
import com.buglist.data.sync.WirePay
import com.buglist.domain.model.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class SyncRepositoryIntegrationTest {
    private lateinit var db: AppDatabase
    private lateinit var passphrase: ByteArray
    private val key = PairingCode.randomKey()
    private val mailbox = mockk<NtfyMailbox>(relaxed = true)

    @Before
    fun setup() {
        System.loadLibrary("sqlcipher")
        passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .openHelperFactory(SupportOpenHelperFactory(passphrase)).build()
    }

    @After
    fun tearDown() {
        db.close()
        passphrase.fill(0)
    }

    private suspend fun prepareGuest(): Long {
        val person = db.personDao().insert(PersonEntity(name = "QA Guest"))
        db.syncDao().upsertPairing(PairingEntity(
            personId = person, role = "GUEST", roomId = PairingCode.randomRoomId(),
            keyMaterial = key, deviceId = "guest", lastPushedHash = "", linkedAt = 1,
            wantedRole = "GUEST", peerRole = "HOST", peerSeenAt = 1
        ))
        val book = WireBook("host", listOf(WireDebt(
            i = "remote-debt", a = 12500, o = true, t = 100, u = 100,
            srl = "BL-20261110-0001", p = listOf(WirePay("remote-payment", 2500, t = 100, u = 100))
        )))
        val plain = Json { encodeDefaults = true }.encodeToString(book).toByteArray()
        coEvery { mailbox.messages(any()) } returns listOf(SnapshotCrypto.encrypt(plain, key))
        return person
    }

    private fun repository(payments: PaymentDao = db.paymentDao()) = SyncRepositoryImpl(
        db.syncDao(), db.debtEntryDao(), payments, mailbox, ChangeSignal(), db
    )

    @Test
    fun failedSnapshotPreservesPreviousDebtsAndPayments() = runTest {
        val person = prepareGuest()
        val previous = db.debtEntryDao().insert(DebtEntryEntity(
            personId = person, amount = 80.0, isOwedToMe = true, date = 1, status = "PARTIAL"
        ))
        db.paymentDao().insert(PaymentEntity(debtEntryId = previous, amount = 10.0))
        val failingPayments = mockk<PaymentDao>()
        coEvery { failingPayments.insert(any()) } throws SQLiteException("injected write failure")

        val result = repository(failingPayments).reconcile(person)

        assertTrue(result is Result.Error)
        val debts = db.syncDao().debtsForPerson(person)
        assertEquals(1, debts.size)
        assertEquals(previous, debts.single().id)
        assertEquals(80.0, debts.single().amount, 0.001)
        assertEquals(10.0, db.syncDao().paymentsForDebt(previous).single().amount, 0.001)
    }

    @Test
    fun receivedBookFlipsDirectionAndPreservesReceiptAndPayments() = runTest {
        val person = prepareGuest()

        assertTrue(repository().reconcile(person) is Result.Success)

        val debt = db.syncDao().debtsForPerson(person).single()
        assertFalse(debt.isOwedToMe)
        assertEquals("BL-20261110-0001", debt.syncSerial)
        assertEquals("PARTIAL", debt.status)
        assertEquals(125.0, debt.amount, 0.001)
        assertEquals(25.0, db.syncDao().paymentsForDebt(debt.id).single().amount, 0.001)
    }
}
