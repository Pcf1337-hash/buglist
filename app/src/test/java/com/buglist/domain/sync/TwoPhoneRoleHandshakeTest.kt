package com.buglist.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two phones share one mailbox list. Each [World.sync] is one "Jetzt holen".
 */
class TwoPhoneRoleHandshakeTest {
    @Test
    fun hostFirstThenViewerLocksBothAndDeliversTheBook() {
        val world = World()
        world.claim('A', host = true)
        world.claim('B', host = false)
        assertEquals("GUEST", world.b.role)
        assertTrue(world.bTookBook)
        world.sync('A')
        assertEquals("HOST", world.a.role)
        assertEquals("GUEST", world.b.role)
        assertTrue(world.log.any { it.from == "A" && it.kind == "role" && it.role == "HOST" })
        assertTrue(world.log.any { it.from == "B" && it.kind == "role" && it.role == "GUEST" })
    }

    @Test
    fun viewerFirstThenHostDoesNotLeaveTheViewerPending() {
        val world = World()
        world.claim('B', host = false)
        assertEquals("PENDING", world.b.role)
        world.claim('A', host = true)
        assertEquals("HOST", world.a.role)
        world.sync('B')
        assertEquals("GUEST", world.b.role)
        assertTrue(world.bTookBook)
        assertFalse(world.b.role == "PENDING")
    }

    @Test
    fun bothHostsStayPendingUntilOneBecomesTheViewer() {
        val world = World()
        world.claim('A', host = true)
        world.claim('B', host = true)
        world.sync('A')
        assertEquals("PENDING", world.a.role)
        assertEquals("PENDING", world.b.role)
        assertEquals(SyncProblem.BOTH_HOST, world.lastProblem)

        world.claim('A', host = false)
        world.sync('B')
        world.sync('A')
        assertEquals("GUEST", world.a.role)
        assertEquals("HOST", world.b.role)
        assertTrue(world.aTookBook)
    }

    @Test
    fun bothViewersStayPendingUntilOneBecomesTheHost() {
        val world = World()
        world.claim('A', host = false)
        world.claim('B', host = false)
        world.sync('A')
        assertEquals("PENDING", world.a.role)
        assertEquals("PENDING", world.b.role)
        assertEquals(SyncProblem.BOTH_VIEW, world.lastProblem)

        world.claim('B', host = true)
        world.sync('A')
        assertEquals("HOST", world.b.role)
        assertEquals("GUEST", world.a.role)
        assertTrue(world.aTookBook)
    }

    @Test
    fun switchingBothToViewerDoesNotLockTwoViewers() {
        val world = World()
        world.claim('A', host = true)
        world.claim('B', host = true)
        world.a = world.a.copy(wanted = "GUEST")
        world.b = world.b.copy(wanted = "GUEST")
        world.sync('A')
        world.sync('B')
        world.sync('A')
        assertEquals("PENDING", world.a.role)
        assertEquals("PENDING", world.b.role)
        assertEquals(SyncProblem.BOTH_VIEW, world.lastProblem)
    }

    @Test
    fun aFailedFetchIsNeverAnEmptyBook() {
        assertEquals(SyncProblem.OFFLINE, visibleProblem(fetchFailed = true, problem = SyncProblem.NO_BOOK))
        assertEquals(SyncProblem.NO_BOOK, visibleProblem(fetchFailed = false, problem = SyncProblem.NO_BOOK))
        assertEquals(null, visibleProblem(fetchFailed = false, problem = null))
    }

    private class World {
        val log = mutableListOf<PeerMessage>()
        var a = PhoneSync("A")
        var b = PhoneSync("B")
        var lastProblem: SyncProblem? = null
        var aTookBook = false
        var bTookBook = false

        fun claim(who: Char, host: Boolean) {
            val wanted = if (host) "HOST" else "GUEST"
            if (who == 'A') a = a.copy(wanted = wanted) else b = b.copy(wanted = wanted)
            sync(who)
        }

        fun sync(who: Char) {
            val phone = if (who == 'A') a else b
            val result = handshake(phone, log.toList())
            log += result.outgoing
            if (result.pushBook) log += PeerMessage(phone.deviceId, "book")
            if (who == 'A') {
                a = result.phone
                if (result.takeBook) aTookBook = true
            } else {
                b = result.phone
                if (result.takeBook) bTookBook = true
            }
            lastProblem = result.problem
        }
    }
}
