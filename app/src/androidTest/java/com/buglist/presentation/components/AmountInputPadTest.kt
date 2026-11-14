package com.buglist.presentation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test

class AmountInputPadTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun accessibleDeleteUsesCurrentInputAndOffersClearAction() {
        compose.setContent {
            var input by remember { mutableStateOf("") }
            AmountInputPad(inputString = input, onInputChange = { input = it })
        }
        listOf("+10", "+20", "+50", "+100", "-10", "-20", "-50", "-100").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("3").performClick()
        compose.onNodeWithText("€ 123").assertExists()
        val delete = compose.onNodeWithContentDescription("Letzte Ziffer löschen")
        delete.performClick()
        compose.onNodeWithText("€ 12").assertExists()
        delete.performClick()
        compose.onNodeWithText("€ 1").assertExists()
        delete.performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.onNodeWithText("€ ").assertExists()
    }
}
