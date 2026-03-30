package com.buglist.domain.usecase

import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Payment
import com.buglist.domain.model.Result
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.PaymentRepository
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Imports a debt list from the BugList share-text format.
 *
 * **Format** (as produced by buildShareText / double-tap on balance):
 * ```
 * Schulden mit NOS – Offen
 *
 * • 30.03.26 | +€ 310,00 [Cal]
 * • 24.03.26 | +€ 230,00 · Beschreibung [Cal, Hase]
 *   ↳ 23.03.26 | € 80,00 · Teilzahlung
 * • 22.03.26 | -€ 100,00
 *
 * Gesamt: +€ 460,00
 * ```
 *
 * **Import behaviour:**
 * 1. All existing debt entries for [personId] are deleted (CASCADE deletes their payments).
 * 2. Each `•` line becomes a new [DebtEntry] (always [DebtStatus.OPEN] initially).
 * 3. Each `  ↳` line under an entry becomes a [Payment] recorded via
 *    [PaymentRepository.insertPaymentAndUpdateStatus], which moves the status to
 *    PARTIAL or PAID as appropriate.
 *
 * @return [Result.Success] with the number of entries imported, or [Result.Error].
 */
class ImportDebtListUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val paymentRepository: PaymentRepository
) {

    suspend operator fun invoke(personId: Long, text: String): Result<Int> {
        val parsed = parseDebtList(text)
        if (parsed.isEmpty()) {
            return Result.Error("Keine Einträge in der Liste gefunden")
        }
        return try {
            // Replace the full list for this person
            debtRepository.deleteAllDebtsForPerson(personId)

            var count = 0
            for (parsedEntry in parsed) {
                val entry = DebtEntry(
                    personId = personId,
                    amount = parsedEntry.amount,
                    isOwedToMe = parsedEntry.isOwedToMe,
                    description = parsedEntry.description,
                    date = parsedEntry.date,
                    status = DebtStatus.OPEN,
                    currency = "EUR"
                )
                val debtId = debtRepository.addDebtEntry(entry)

                // Apply payments in chronological order to correctly compute PARTIAL / PAID
                val sortedPayments = parsedEntry.payments.sortedBy { it.date }
                var cumulativePaid = 0.0
                for (payment in sortedPayments) {
                    cumulativePaid += payment.amount
                    val newRemaining = maxOf(0.0, parsedEntry.amount - cumulativePaid)
                    val newStatus = if (newRemaining < 0.001) DebtStatus.PAID else DebtStatus.PARTIAL
                    paymentRepository.insertPaymentAndUpdateStatus(
                        payment = Payment(
                            debtEntryId = debtId,
                            amount = payment.amount,
                            note = payment.note,
                            date = payment.date
                        ),
                        newStatus = newStatus.name
                    )
                }
                count++
            }
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error("Import fehlgeschlagen: ${e.message}", e)
        }
    }
}

// ---------------------------------------------------------------------------
// Parsing helpers
// ---------------------------------------------------------------------------

/**
 * Parsed intermediate representation of a single debt entry line (and its payment lines).
 */
internal data class ParsedDebtEntry(
    val date: Long,
    val amount: Double,
    val isOwedToMe: Boolean,
    val description: String?,
    val payments: List<ParsedPayment>
)

internal data class ParsedPayment(
    val date: Long,
    val amount: Double,
    val note: String?
)

/**
 * Regex for an entry line:
 * `• DD.MM.YY | [+/-][€/$|£|CHF] [1.234,56][ · description][ [tag1, tag2]]`
 */
private val ENTRY_REGEX = Regex(
    """^•\s+(\d{2}\.\d{2}\.\d{2})\s+\|\s+([+\-])(€|\$|£|CHF)\s+([0-9.,]+)(.*)$"""
)

/**
 * Regex for a payment line:
 * `  ↳ DD.MM.YY | [€/$|£|CHF] [1.234,56][ · note]`
 *
 * Note: uses flexible leading whitespace to handle copy-paste variants.
 */
private val PAYMENT_REGEX = Regex(
    """^\s+↳\s+(\d{2}\.\d{2}\.\d{2})\s+\|\s+(€|\$|£|CHF)\s+([0-9.,]+)(.*)$"""
)

private val DATE_FORMAT = SimpleDateFormat("dd.MM.yy", Locale.GERMAN)

/**
 * Parses a BugList share-text into a list of [ParsedDebtEntry] objects.
 *
 * Lines that are not recognised (headers, empty lines, Gesamt) are silently skipped.
 */
internal fun parseDebtList(text: String): List<ParsedDebtEntry> {
    val result = mutableListOf<ParsedDebtEntry>()
    var currentEntry: ParsedDebtEntry? = null
    var currentPayments = mutableListOf<ParsedPayment>()

    for (line in text.lines()) {
        val entryMatch = ENTRY_REGEX.matchEntire(line.trim())
        val paymentMatch = PAYMENT_REGEX.matchEntire(line)

        when {
            entryMatch != null -> {
                // Flush previous entry
                currentEntry?.let { result.add(it.copy(payments = currentPayments.toList())) }
                currentPayments = mutableListOf()

                val (dateStr, sign, _, amountStr, meta) = entryMatch.destructured
                val date = parseDate(dateStr)
                val amount = parseGermanAmount(amountStr)
                if (amount <= 0.0) continue // skip invalid amounts

                val (description, _) = parseMeta(meta.trim())
                currentEntry = ParsedDebtEntry(
                    date = date,
                    amount = amount,
                    isOwedToMe = sign == "+",
                    description = description,
                    payments = emptyList()
                )
            }

            paymentMatch != null -> {
                val (dateStr, _, amountStr, meta) = paymentMatch.destructured
                val date = parseDate(dateStr)
                val amount = parseGermanAmount(amountStr)
                if (amount <= 0.0) continue

                val note = meta.trim()
                    .removePrefix("·").trim()
                    .takeIf { it.isNotBlank() }

                currentPayments.add(ParsedPayment(date, amount, note))
            }
            // else: header / empty / Gesamt line — skip
        }
    }

    // Flush last entry
    currentEntry?.let { result.add(it.copy(payments = currentPayments.toList())) }
    return result
}

/** Parses a date string `DD.MM.YY` → Unix ms. Falls back to now on failure. */
private fun parseDate(dateStr: String): Long =
    try { DATE_FORMAT.parse(dateStr)?.time ?: System.currentTimeMillis() }
    catch (_: Exception) { System.currentTimeMillis() }

/**
 * Converts a German-formatted number string to [Double].
 * Handles both `1.234,56` (thousands-dot, decimal-comma) and `1234,56` / `1234.56`.
 */
private fun parseGermanAmount(str: String): Double =
    try {
        // German locale: dots = thousands separator, comma = decimal separator
        str.replace(".", "").replace(",", ".").toDouble()
    } catch (_: Exception) { 0.0 }

/**
 * Extracts optional description and tags from the trailing metadata of an entry line.
 *
 * Input examples:
 * - ` · Some description [tag1, tag2]`
 * - ` [tag1, tag2]`
 * - ` · Just a description`
 * - `` (empty)
 *
 * @return `Pair<description, tags>` — either or both may be null/empty.
 */
private fun parseMeta(meta: String): Pair<String?, List<String>> {
    var rest = meta
    val tags = mutableListOf<String>()

    // Extract trailing `[...]` tags block
    val tagsMatch = Regex("""\[([^\]]+)]\s*$""").find(rest)
    if (tagsMatch != null) {
        tags += tagsMatch.groupValues[1].split(",").map { it.trim() }.filter { it.isNotBlank() }
        rest = rest.substring(0, tagsMatch.range.first).trim()
    }

    // Extract description after optional ` · ` or `· `
    val description = rest.removePrefix("·").trim().takeIf { it.isNotBlank() }

    return Pair(description, tags)
}
