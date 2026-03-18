package com.buglist.presentation.add_person

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily

/**
 * Bottom sheet for adding a new person to the ledger.
 *
 * @param onDismiss  Called when sheet is closed without saving.
 * @param onSaved    Called after successful person creation, with the new person's ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonSheet(
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AddPersonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AddPersonUiState.Success) {
            viewModel.reset()
            onSaved((uiState as AddPersonUiState.Success).personId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BugListColors.Surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.add_person_title).uppercase(),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BugListColors.Gold
            )
            Spacer(Modifier.height(16.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BugListColors.Gold,
                unfocusedBorderColor = BugListColors.Divider,
                focusedTextColor = BugListColors.Platinum,
                unfocusedTextColor = BugListColors.Platinum,
                cursorColor = BugListColors.Gold
            )
            val fieldShape = RoundedCornerShape(8.dp)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(stringResource(R.string.add_person_name_hint),
                        fontFamily = RobotoCondensedFontFamily, color = BugListColors.Muted)
                },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = {
                    Text(stringResource(R.string.add_person_phone_hint),
                        fontFamily = RobotoCondensedFontFamily, color = BugListColors.Muted)
                },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = {
                    Text(stringResource(R.string.add_person_notes_hint),
                        fontFamily = RobotoCondensedFontFamily, color = BugListColors.Muted)
                },
                minLines = 2,
                maxLines = 3,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (uiState is AddPersonUiState.Error) {
                Text(
                    text = (uiState as AddPersonUiState.Error).message,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 13.sp,
                    color = BugListColors.DebtRed
                )
                Spacer(Modifier.height(8.dp))
            }

            GoldButton(
                text = stringResource(R.string.action_save),
                onClick = { viewModel.savePerson(name, phone, notes) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
