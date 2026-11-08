package com.buglist.domain.sync

/**
 * One debt in a coupled book, already expressed in the local user's direction.
 * Tombstones live in [deleted] and [removedPayments]. Nothing here is a visible log.
 */
data class BookDebt(
    val syncId: String,
    val amountCents: Long,
    val currency: String,
    val owedToMe: Boolean,
    val note: String?,
    val date: Long,
    val dueDate: Long?,
    val updatedAt: Long,
    val cancelled: Boolean,
    val deleted: Boolean,
    val payments: List<BookPayment>,
    val removedPayments: Map<String, Long>,
    /** Host receipt number. Null until the first publish fills it. */
    val serial: String? = null
)

data class BookPayment(
    val syncId: String,
    val amountCents: Long,
    val note: String?,
    val date: Long,
    val updatedAt: Long
)

/**
 * Merges two current books. The newer debt fields win. Payments are combined by id
 * and dropped only when a newer removal says so. No history list is produced.
 */
fun mergeBooks(local: List<BookDebt>, remote: List<BookDebt>): List<BookDebt> {
    val localById = local.associateBy { it.syncId }
    val remoteById = remote.associateBy { it.syncId }
    return (localById.keys + remoteById.keys).map { id ->
        val mine = localById[id]
        val theirs = remoteById[id]
        when {
            mine == null -> theirs!!
            theirs == null -> mine
            else -> mergeOne(mine, theirs)
        }
    }.sortedBy { it.syncId }
}

private fun mergeOne(local: BookDebt, remote: BookDebt): BookDebt {
    val newer = if (remote.updatedAt > local.updatedAt) remote else local
    val removed = HashMap<String, Long>(local.removedPayments.size + remote.removedPayments.size)
    putNewer(removed, local.removedPayments)
    putNewer(removed, remote.removedPayments)
    val payments = (local.payments + remote.payments)
        .groupBy { it.syncId }
        .map { (_, versions) -> versions.maxBy { it.updatedAt } }
        .filter { payment -> (removed[payment.syncId] ?: -1L) < payment.updatedAt }
        .sortedBy { it.syncId }
    val deleted = when {
        local.deleted && remote.deleted -> true
        local.deleted && local.updatedAt >= remote.updatedAt -> true
        remote.deleted && remote.updatedAt >= local.updatedAt -> true
        else -> false
    }
    return newer.copy(
        deleted = deleted,
        payments = if (deleted) emptyList() else payments,
        removedPayments = removed
    )
}

private fun putNewer(into: MutableMap<String, Long>, from: Map<String, Long>) {
    for ((id, at) in from) {
        val current = into[id]
        if (current == null || at > current) into[id] = at
    }
}
