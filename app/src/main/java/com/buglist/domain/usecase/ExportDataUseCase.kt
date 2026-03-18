package com.buglist.domain.usecase

import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.PersonWithBalance
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.PersonRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case that exports all debt data as a CSV string.
 *
 * CSV format (semicolon-delimited for European locale compatibility):
 * ```
 * Person;Debt ID;Direction;Amount;Currency;Status;Paid;Remaining;Description;Date;Due Date;Payments
 * Alice;1;OWED_TO_ME;100.00;EUR;PARTIAL;50.00;50.00;Lunch;2026-01-15;;2026-01-15:50.00 (Cash)
 * ```
 *
 * The CSV includes all debts across all persons, including payment history per row.
 * Returns the full CSV as a [String] for sharing via Android Share Intent.
 */
class ExportDataUseCase @Inject constructor(
    private val personRepository: PersonRepository,
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()

        // Header
        sb.appendLine("Person;Debt ID;Direction;Original Amount;Currency;Status;Total Paid;Remaining;Description;Date;Due Date;Payment History")

        val persons = personRepository.getAllPersonsWithBalance().first()

        for (personWithBalance in persons) {
            val person = personWithBalance.person
            val debtsWithPayments = debtRepository
                .getDebtEntriesWithPaymentsForPerson(person.id)
                .first()

            for (dwp in debtsWithPayments) {
                val entry = dwp.entry
                val direction = if (entry.isOwedToMe) "OWED_TO_ME" else "I_OWE"
                val dateStr = dateFormat.format(Date(entry.date))
                val dueDateStr = entry.dueDate?.let { dateFormat.format(Date(it)) } ?: ""
                val description = entry.description?.replace(";", ",") ?: ""

                val paymentHistory = dwp.payments.joinToString("|") { payment ->
                    val payDate = dateFormat.format(Date(payment.date))
                    val note = payment.note?.replace(";", ",") ?: ""
                    "$payDate:${payment.amount} ($note)"
                }

                sb.appendLine(
                    "${csvEscape(person.name)};${entry.id};$direction;" +
                        "${entry.amount};${entry.currency};${entry.status.name};" +
                        "${dwp.totalPaid};${dwp.remaining};" +
                        "${csvEscape(description)};$dateStr;$dueDateStr;" +
                        paymentHistory
                )
            }
        }

        return sb.toString()
    }

    private fun csvEscape(value: String): String =
        if (value.contains(';') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
