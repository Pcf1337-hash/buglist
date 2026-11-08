package com.buglist.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Visible receipt number. Synced copies keep the host's number in [synced]
 * so a fresh local row id does not mint BL-…0002, 0003, 0004.
 */
fun debtSerial(createdAt: Long, localId: Long, synced: String? = null): String {
    if (!synced.isNullOrBlank()) return synced
    val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(createdAt))
    return "BL-$day-${"%04d".format(localId % 10000)}"
}
