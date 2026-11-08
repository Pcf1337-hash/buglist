package com.buglist.data.sync

import com.buglist.domain.sync.BookDebt
import com.buglist.domain.sync.BookPayment
import kotlinx.serialization.Serializable

@Serializable
data class SyncNote(
    val t: String = "book",
    val from: String = "",
    val role: String? = null,
    val debts: List<WireDebt> = emptyList()
)

@Serializable
data class WireBook(
    val from: String,
    val debts: List<WireDebt>
)

@Serializable
data class WireDebt(
    val i: String,
    val a: Long,
    val c: String = "EUR",
    val o: Boolean,
    val n: String? = null,
    val t: Long,
    val due: Long? = null,
    val u: Long,
    val x: Boolean = false,
    val cancelled: Boolean = false,
    val p: List<WirePay> = emptyList(),
    val rp: List<WireRemoval> = emptyList(),
    /** Host receipt number. Missing on snapshots from 51bcda8. */
    val srl: String? = null
)

@Serializable
data class WirePay(
    val i: String,
    val a: Long,
    val n: String? = null,
    val t: Long,
    val u: Long
)

@Serializable
data class WireRemoval(
    val i: String,
    val u: Long
)

fun BookDebt.toWire(): WireDebt = WireDebt(
    i = syncId,
    a = amountCents,
    c = currency,
    o = owedToMe,
    n = note,
    t = date,
    due = dueDate,
    u = updatedAt,
    x = deleted,
    cancelled = cancelled,
    p = payments.map { WirePay(it.syncId, it.amountCents, it.note, it.date, it.updatedAt) },
    rp = removedPayments.map { (id, at) -> WireRemoval(id, at) },
    srl = serial
)

/** [owedToPublisher] is the sender's direction, so the receiver flips it. */
fun WireDebt.toLocalBook(): BookDebt = BookDebt(
    syncId = i,
    amountCents = a,
    currency = c,
    owedToMe = !o,
    note = n,
    date = t,
    dueDate = due,
    updatedAt = u,
    cancelled = cancelled,
    deleted = x,
    payments = p.map { BookPayment(it.i, it.a, it.n, it.t, it.u) },
    removedPayments = rp.associate { it.i to it.u },
    serial = srl
)
