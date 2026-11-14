package com.buglist.presentation.components

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.buglist.domain.model.*
import org.junit.Rule
import org.junit.Test

class RepaymentCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun amountTapRevealsHistoryWhileCollapsedRowShowsOnlyTheRecordedPayment() {
        val debt = DebtEntryWithPayments.from(
            DebtEntry(id = 1, personId = 1, amount = 100.0, isOwedToMe = true,
                date = 100L, description = "Ursprüngliche Schuld", status = DebtStatus.PARTIAL),
            listOf(Payment(id = 1, debtEntryId = 1, amount = 20.0, date = 1000L),
                Payment(id = 2, debtEntryId = 1, amount = 50.0, date = 2000L))
        )
        compose.setContent {
            var expanded by remember { mutableStateOf(false) }
            RepaymentCard(RepaymentHistoryItem(debt, debt.payments[1]), expanded, { expanded = !expanded })
        }
        compose.onNodeWithText("Tilgung erhalten").assertExists()
        compose.onNodeWithText("Ursprüngliche Schuld").assertDoesNotExist()
        compose.onNodeWithText("20,00 €").assertDoesNotExist()
        compose.onNodeWithText("100,00 €").assertDoesNotExist()
        compose.onNodeWithText("50,00 €").performClick()
        compose.onNodeWithText("Ursprüngliche Schuld").assertExists()
        compose.onNodeWithText("20,00 €").assertExists()
    }

    @Test fun legacyMarkDoesNotPretendToHaveAPaymentDate() {
        val debt = DebtEntryWithPayments.from(
            DebtEntry(id = 1, personId = 1, amount = 100.0, isOwedToMe = true,
                date = 100L, status = DebtStatus.PAID), emptyList())
        compose.setContent { RepaymentCard(RepaymentHistoryItem(debt, null), false, {}) }
        compose.onNodeWithText("Als bezahlt markiert").assertExists()
        compose.onNodeWithText("Bezahldatum nicht erfasst").assertExists()
    }
}
