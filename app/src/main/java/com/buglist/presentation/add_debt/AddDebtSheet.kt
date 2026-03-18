package com.buglist.presentation.add_debt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.domain.model.DebtEntry
import com.buglist.presentation.components.AmountInputPad
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily

/**
 * Bottom sheet for entering a new debt entry or editing an existing one.
 *
 * Uses [AmountInputPad] for amount entry, a direction toggle, and an optional
 * description field. Date defaults to now for new entries.
 *
 * When [existingDebt] is non-null the sheet opens in edit mode: prefilled values,
 * title "EINTRAG BEARBEITEN", and the save button calls [AddDebtViewModel.updateDebt].
 *
 * See L-079 for scroll/padding rules.
 *
 * @param personId      Person this debt is for.
 * @param existingDebt  Non-null when editing an existing entry.
 * @param onDismiss     Called when the sheet should be closed.
 * @param onSaved       Called after successful save or update.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDebtSheet(
    personId: Long,
    existingDebt: DebtEntry? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddDebtViewModel = hiltViewModel()
) {
    val isEditMode = existingDebt != null
    val uiState by viewModel.debtUiState.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedTagIds by viewModel.selectedTagIds.collectAsStateWithLifecycle()

    // 80% swipe-down threshold: track sheet height and current drag offset.
    // confirmValueChange blocks the hide transition unless the sheet has been
    // dragged ≥ 80% of its own height (or the layout hasn't been measured yet).
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    val dismissAllowed = remember { mutableStateOf(false) }
    val sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            targetValue != SheetValue.Hidden || dismissAllowed.value
        }
    )

    // Observe the sheet's drag offset via snapshotFlow and update dismissAllowed.
    // requireOffset() is backed by snapshot state so snapshotFlow re-emits on every frame.
    LaunchedEffect(sheetState, sheetHeightPx) {
        if (sheetHeightPx <= 0f) return@LaunchedEffect
        snapshotFlow { runCatching { sheetState.requireOffset() }.getOrDefault(0f) }
            .collect { offset -> dismissAllowed.value = offset >= sheetHeightPx * 0.8f }
    }

    // In edit mode: pre-select the tags attached to the existing debt entry.
    LaunchedEffect(existingDebt) {
        if (existingDebt != null) {
            viewModel.loadTagsForExistingDebt(existingDebt)
        }
    }

    // For edit mode: the UI toggle is inverted vs DB (see L-080).
    // isOwedToMe=true in DB means "schuldet mir" → toggle unchecked (false in UI).
    // Initialise from existingDebt when editing.
    val initialAmountInput = remember(existingDebt) {
        existingDebt?.amount?.let { amount ->
            if (amount == kotlin.math.floor(amount)) {
                amount.toLong().toString()
            } else {
                String.format(java.util.Locale.ROOT, "%.2f", amount).replace(".", ",")
            }
        } ?: ""
    }
    val initialIsOwedToMeUiToggle = remember(existingDebt) {
        // UI toggle: false = "SCHULDET MIR" (DB isOwedToMe=true), true = "ICH SCHULDE" (DB isOwedToMe=false)
        existingDebt?.let { !it.isOwedToMe } ?: false
    }
    val initialDescription = remember(existingDebt) { existingDebt?.description ?: "" }

    var amountInput by rememberSaveable { mutableStateOf(initialAmountInput) }
    var isOwedToMe by rememberSaveable { mutableStateOf(initialIsOwedToMeUiToggle) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }

    LaunchedEffect(uiState) {
        if (uiState is AddDebtUiState.Success) {
            viewModel.resetDebtState()
            onSaved()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BugListColors.Surface,
        modifier = Modifier.onSizeChanged { size -> sheetHeightPx = size.height.toFloat() }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp)
        ) {
            // Header — title differs by mode
            Text(
                text = if (isEditMode)
                    stringResource(R.string.edit_debt_title)
                else
                    stringResource(R.string.add_debt_title).uppercase(),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BugListColors.Gold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Amount pad
            AmountInputPad(
                inputString = amountInput,
                onInputChange = { amountInput = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Direction toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (!isOwedToMe)
                        stringResource(R.string.add_debt_direction_owed_to_me)
                    else
                        stringResource(R.string.add_debt_direction_i_owe),
                    fontFamily = RobotoCondensedFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    // "SCHULDET MIR" (isOwedToMe=false in UI = DB true) → ROT
                    // "ICH SCHULDE" (isOwedToMe=true in UI = DB false) → GRÜN
                    color = if (!isOwedToMe) BugListColors.DebtRed else BugListColors.DebtGreen,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = isOwedToMe,
                    onCheckedChange = { isOwedToMe = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = BugListColors.DebtGreen,
                        uncheckedTrackColor = BugListColors.DebtRed
                    )
                )
            }

            // Tag chips — shown when there are tags to display
            if (allTags.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.add_debt_tags_label),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = BugListColors.Muted,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        val isSelected = tag.id in selectedTagIds
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleTag(tag.id) },
                            label = {
                                Text(
                                    text = tag.name,
                                    fontFamily = RobotoCondensedFontFamily,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BugListColors.Gold,
                                selectedLabelColor = Color.Black,
                                containerColor = BugListColors.Surface,
                                labelColor = BugListColors.Platinum
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BugListColors.Divider,
                                selectedBorderColor = BugListColors.Gold
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Description (optional)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = {
                    Text(
                        stringResource(R.string.add_debt_description_hint),
                        fontFamily = RobotoCondensedFontFamily,
                        color = BugListColors.Muted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BugListColors.Gold,
                    unfocusedBorderColor = BugListColors.Divider,
                    focusedTextColor = BugListColors.Platinum,
                    unfocusedTextColor = BugListColors.Platinum,
                    cursorColor = BugListColors.Gold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Error message
            if (uiState is AddDebtUiState.Error) {
                Text(
                    text = (uiState as AddDebtUiState.Error).message,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 13.sp,
                    color = BugListColors.DebtRed,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            // Save button — label differs by mode
            GoldButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    if (existingDebt != null) {
                        viewModel.updateDebt(
                            existingEntry = existingDebt,
                            amountString = amountInput,
                            isOwedToMe = !isOwedToMe,
                            date = existingDebt.date,
                            dueDate = existingDebt.dueDate,
                            description = description
                        )
                    } else {
                        viewModel.saveDebt(
                            personId = personId,
                            amountString = amountInput,
                            isOwedToMe = !isOwedToMe,
                            date = System.currentTimeMillis(),
                            dueDate = null,
                            description = description
                        )
                    }
                },
                enabled = amountInput.isNotBlank() && amountInput != "0",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
