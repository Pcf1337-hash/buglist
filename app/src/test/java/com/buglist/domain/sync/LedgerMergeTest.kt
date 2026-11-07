package com.buglist.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerMergeTest {

    @Test
    fun newerDebtFieldsWinAndPaymentsCombine() {
        val local = debt(updatedAt = 10, payments = listOf(pay("p1", 500, 10)))
        val remote = local.copy(
            updatedAt = 20,
            note = "neu",
            payments = listOf(pay("p2", 200, 20))
        )
        val merged = mergeBooks(listOf(local), listOf(remote)).single()
        assertEquals("neu", merged.note)
        assertEquals(setOf("p1", "p2"), merged.payments.map { it.syncId }.toSet())
        assertFalse(merged.deleted)
    }

    @Test
    fun newerRemovalDropsPayment() {
        val local = debt(
            updatedAt = 10,
            payments = listOf(pay("p1", 500, 10)),
            removedPayments = emptyMap()
        )
        val remote = local.copy(updatedAt = 11, removedPayments = mapOf("p1" to 30))
        val merged = mergeBooks(listOf(local), listOf(remote)).single()
        assertTrue(merged.payments.isEmpty())
    }

    @Test
    fun newerTombstoneDeletesTheDebt() {
        val local = debt(updatedAt = 10)
        val remote = local.copy(updatedAt = 40, deleted = true, payments = emptyList())
        val merged = mergeBooks(listOf(local), listOf(remote)).single()
        assertTrue(merged.deleted)
    }

    @Test
    fun olderRemoteDoesNotOverwrite() {
        val local = debt(updatedAt = 50, note = "hier")
        val remote = local.copy(updatedAt = 20, note = "alt")
        val merged = mergeBooks(listOf(local), listOf(remote)).single()
        assertEquals("hier", merged.note)
    }

    private fun debt(
        updatedAt: Long,
        note: String? = null,
        payments: List<BookPayment> = emptyList(),
        removedPayments: Map<String, Long> = emptyMap(),
        deleted: Boolean = false
    ) = BookDebt(
        syncId = "d1",
        amountCents = 1000,
        currency = "EUR",
        owedToMe = true,
        note = note,
        date = 1,
        dueDate = null,
        updatedAt = updatedAt,
        cancelled = false,
        deleted = deleted,
        payments = payments,
        removedPayments = removedPayments
    )

    private fun pay(id: String, cents: Long, at: Long) = BookPayment(id, cents, null, at, at)
}
