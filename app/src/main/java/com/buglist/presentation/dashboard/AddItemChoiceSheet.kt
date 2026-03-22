package com.buglist.presentation.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily

/**
 * Bottom sheet shown when the user taps the Dashboard FAB (+).
 *
 * Presents two mutually exclusive actions:
 * - Add a person (crew member)
 * - Add a divider (visual separator with optional label)
 *
 * @param onAddPerson   Callback to open [com.buglist.presentation.add_person.AddPersonSheet].
 * @param onAddDivider  Callback to open [com.buglist.presentation.add_divider.AddDividerSheet].
 * @param onDismiss     Called when the sheet is dismissed without selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemChoiceSheet(
    onAddPerson: () -> Unit,
    onAddDivider: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BugListColors.Surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

            Text(
                text = stringResource(R.string.add_item_choice_title),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BugListColors.Platinum
            )

            Spacer(Modifier.height(20.dp))

            // Primary: add person
            GoldButton(
                text = stringResource(R.string.add_item_choice_person),
                onClick = {
                    onDismiss()
                    onAddPerson()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Secondary: add divider (outlined style)
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onAddDivider()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BugListColors.Gold
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, BugListColors.Gold
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.add_item_choice_divider),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
