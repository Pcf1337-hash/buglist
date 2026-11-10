package com.buglist.data.repository

import com.buglist.data.local.AppDatabase
import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.dao.SyncDao
import com.buglist.data.local.entity.PairingEntity
import com.buglist.data.sync.ChangeSignal
import com.buglist.data.sync.NtfyMailbox
import com.buglist.data.sync.PairingCode
import com.buglist.data.sync.SnapshotCrypto
import com.buglist.data.sync.SyncNote
import com.buglist.data.sync.WireBook
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class SyncRepositoryImplTest {
    private val syncDao = mockk<SyncDao>(relaxed = true)
    private val mailbox = mockk<NtfyMailbox>(relaxed = true)
    private val json = Json { encodeDefaults = true }
    private val key = PairingCode.randomKey()
    private val book = json.encodeToString(WireBook(from = "host", debts = emptyList()))
    private val pairing = PairingEntity(
        personId = 1, role = "HOST", roomId = PairingCode.randomRoomId(),
        keyMaterial = key, deviceId = "host", lastPushedHash = SnapshotCrypto.sha256(book.toByteArray()),
        linkedAt = 1, wantedRole = "HOST", peerRole = "GUEST", peerSeenAt = 1
    )

    @Test
    fun unchangedBookIsRepublishedWhenMailboxCacheHasExpired() = runTest {
        coEvery { syncDao.getPairing(1) } returns pairing
        coEvery { mailbox.messages(any()) } returns emptyList()

        repository().reconcile(1)

        // One role message and one book, despite the unchanged saved hash.
        coVerify(exactly = 2) { mailbox.publish(any(), any()) }
        coVerify(exactly = 1) { syncDao.updatePushedHash(1, pairing.lastPushedHash) }
    }

    @Test
    fun unchangedBookStillInMailboxIsNotRepublished() = runTest {
        coEvery { syncDao.getPairing(1) } returns pairing
        coEvery { mailbox.messages(any()) } returns listOf(SnapshotCrypto.encrypt(book.toByteArray(), key))

        repository().reconcile(1)

        coVerify(exactly = 1) { mailbox.publish(any(), any()) }
        coVerify(exactly = 0) { syncDao.updatePushedHash(any(), any()) }
    }

    @Test
    fun peerRoleMessageDoesNotCountAsRetainedBook() = runTest {
        coEvery { syncDao.getPairing(1) } returns pairing
        val role = json.encodeToString(SyncNote(t = "role", from = "peer", role = "GUEST"))
        coEvery { mailbox.messages(any()) } returns listOf(SnapshotCrypto.encrypt(role.toByteArray(), key))

        repository().reconcile(1)

        coVerify(exactly = 2) { mailbox.publish(any(), any()) }
    }

    @Test
    fun repeatedPollingDoesNotRepublishRetainedRoleOrBook() = runTest {
        coEvery { syncDao.getPairing(1) } returns pairing
        coEvery { mailbox.messages(any()) } returns listOf(
            SnapshotCrypto.encrypt(book.toByteArray(), key),
            encryptedNote("role", "HOST")
        )

        repeat(3) { repository().reconcile(1) }

        coVerify(exactly = 0) { mailbox.publish(any(), any()) }
    }

    @Test
    fun latestDifferentRoleMustBeSupersededEvenIfMatchingOlderRoleExists() = runTest {
        coEvery { syncDao.getPairing(1) } returns pairing
        coEvery { mailbox.messages(any()) } returns listOf(
            SnapshotCrypto.encrypt(book.toByteArray(), key),
            encryptedNote("role", "HOST"), encryptedNote("role", "GUEST")
        )

        repository().reconcile(1)

        coVerify(exactly = 1) { mailbox.publish(any(), any()) }
    }

    @Test
    fun waitingPhoneDoesNotRepeatRetainedHello() = runTest {
        coEvery { syncDao.getPairing(1) } returns pairing.copy(
            role = "PENDING", wantedRole = null, peerRole = null, peerSeenAt = 0)
        coEvery { mailbox.messages(any()) } returns listOf(encryptedNote("hello", null))

        repository().reconcile(1)

        coVerify(exactly = 0) { mailbox.publish(any(), any()) }
    }

    private fun encryptedNote(type: String, role: String?): String = SnapshotCrypto.encrypt(
        json.encodeToString(SyncNote(t = type, from = "host", role = role)).toByteArray(), key)

    private fun repository() = SyncRepositoryImpl(
        syncDao, mockk<DebtEntryDao>(), mockk<PaymentDao>(), mailbox, ChangeSignal(), mockk<AppDatabase>()
    )
}
