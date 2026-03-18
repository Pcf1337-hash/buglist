package com.buglist.domain.usecase

import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Payment
import com.buglist.domain.model.Result
import com.buglist.domain.model.SettledEntry
import com.buglist.domain.model.SettlementResult
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Verrechnet einen Gesamtbetrag gegen die offenen Schulden einer Person,
 * beginnend mit dem ältesten Eintrag (FIFO – First In, First Out).
 *
 * Verrechnungsregel:
 * - Nur OPEN- und PARTIAL-Einträge werden berücksichtigt.
 * - Sortierung nach `date` ASC: der älteste Eintrag wird zuerst abgebaut.
 * - Für jeden verrechneten Betrag wird ein [Payment]-Eintrag angelegt.
 * - Der Gesamtbetrag kann kleiner als alle offenen Schulden sein (partiell),
 *   gleich (exakte Tilgung) oder größer (alle PAID, remainingBudget > 0).
 *
 * @param personId      Die Person gegen die verrechnet wird.
 * @param totalAmount   Der Gesamtbetrag der Tilgung — muss > 0 sein.
 * @param isOwedToMe    true = Person tilgt ihre Schulden bei mir (Normalfall);
 *                      false = ich tilge meine Schulden bei der Person.
 * @return [Result.Success] mit [SettlementResult], oder [Result.Error] bei Validierungsfehlern.
 */
class SettleDebtsUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(
        personId: Long,
        totalAmount: Double,
        isOwedToMe: Boolean
    ): Result<SettlementResult> {
        if (personId <= 0) {
            return Result.Error("Invalid person ID")
        }
        if (totalAmount <= 0.0) {
            return Result.Error("Settlement amount must be greater than 0")
        }

        // Load open/partial debts, oldest first (FIFO)
        val openDebts = debtRepository.getOpenDebtsForPerson(personId, isOwedToMe)

        if (openDebts.isEmpty()) {
            return Result.Success(
                SettlementResult(
                    settledEntries = emptyList(),
                    partialEntry = null,
                    totalSettled = 0.0,
                    remainingBudget = totalAmount
                )
            )
        }

        // FIFO allocation — build a settlement plan without touching the DB yet
        var remainingBudget = totalAmount
        val fullySettled = mutableListOf<SettledEntry>()
        var partiallySettled: SettledEntry? = null

        for (debtWithPayments in openDebts) {
            if (remainingBudget <= 0.001) break

            val debtRemaining = debtWithPayments.remaining
            if (debtRemaining <= 0.001) continue // already paid (safety guard)

            val amountToApply = minOf(remainingBudget, debtRemaining)
            val newRemaining = debtRemaining - amountToApply
            val newStatus = if (newRemaining < 0.001) DebtStatus.PAID else DebtStatus.PARTIAL

            val settled = SettledEntry(
                debtEntry = debtWithPayments.entry,
                amountPaid = amountToApply,
                newStatus = newStatus
            )

            if (newStatus == DebtStatus.PAID) {
                fullySettled.add(settled)
            } else {
                partiallySettled = settled
            }

            remainingBudget -= amountToApply
        }

        val totalSettled = totalAmount - maxOf(0.0, remainingBudget)

        // Persist — one payment per affected entry, each atomically updates the entry status
        return try {
            for (settled in fullySettled) {
                val payment = Payment(
                    debtEntryId = settled.debtEntry.id,
                    amount = settled.amountPaid,
                    note = null,
                    date = System.currentTimeMillis()
                )
                paymentRepository.insertPaymentAndUpdateStatus(payment, settled.newStatus.name)
            }
            partiallySettled?.let { settled ->
                val payment = Payment(
                    debtEntryId = settled.debtEntry.id,
                    amount = settled.amountPaid,
                    note = null,
                    date = System.currentTimeMillis()
                )
                paymentRepository.insertPaymentAndUpdateStatus(payment, settled.newStatus.name)
            }

            Result.Success(
                SettlementResult(
                    settledEntries = fullySettled,
                    partialEntry = partiallySettled,
                    totalSettled = totalSettled,
                    remainingBudget = maxOf(0.0, remainingBudget)
                )
            )
        } catch (e: Exception) {
            Result.Error("Settlement failed: ${e.message}", e)
        }
    }
}
