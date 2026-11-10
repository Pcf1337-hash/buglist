package com.buglist.presentation.components

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.buglist.domain.model.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PaidDebtCardTest {
    @get:Rule val compose = createComposeRule()
    private fun time(day: Int) = LocalDateTime.of(2026, 11, day, 15, 30)
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun collapsedCardShowsCompletionDateAndExpandsIntoRemainingBalances() {
        val debt = DebtEntryWithPayments.from(DebtEntry(id = 1, personId = 1, amount = 100.0,
            isOwedToMe = true, date = time(1), status = DebtStatus.PAID, description = "Alte Schuld"),
            listOf(Payment(id = 1, debtEntryId = 1, amount = 60.0, date = time(5)),
                Payment(id = 2, debtEntryId = 1, amount = 40.0, date = time(10))))
        compose.setContent {
            var expanded by remember { mutableStateOf(false) }
            PaidDebtCard(debt, expanded = expanded, onClick = { expanded = !expanded })
        }
        compose.onNodeWithText("VOLLSTÄNDIG BEZAHLT").assertExists()
        compose.onNodeWithText("10.11.2026 · 15:30").assertExists()
        compose.onNodeWithText("Schuld vom 01.11.2026").assertExists()
        compose.onNodeWithText("Zahlungen anzeigen ▾").performClick()
        compose.onNodeWithText("Danach noch offen: 40,00 €").assertExists()
        compose.onNodeWithText("Damit vollständig bezahlt · Rest 0,00").assertExists()
    }

    @Test fun legacyPaidDebtShowsMissingDateExplicitly() {
        val debt = DebtEntryWithPayments.from(DebtEntry(id = 1, personId = 1, amount = 100.0,
            isOwedToMe = true, date = time(1), status = DebtStatus.PAID), emptyList())
        compose.setContent { PaidDebtCard(debt) }
        compose.onNodeWithText("Bezahldatum nicht erfasst").assertExists()
        compose.onNodeWithText("VOLLSTÄNDIG BEZAHLT").assertExists()
    }
}
