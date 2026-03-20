package com.buglist.presentation.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.domain.model.DebtStatus
import com.buglist.presentation.components.AmountInputPad
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.components.formatAmount
import com.buglist.presentation.theme.BebasNeueFontFamily
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet for settling (tilging) debts of a person against a total amount using FIFO.
 *
 * Features:
 * - Shows total open amount and number of open entries.
 * - Expandable list of individual open debt entries.
 * - Live preview list updates as the user types an amount.
 * - Confirmation dialog before committing.
 * - Success state shows which entries were settled.
 * - Toast confirmation fires immediately after settlement via [SettlementViewModel.toastEvent].
 *
 * The sheet closes via swipe-down or the cancel button. With [skipPartiallyExpanded = true]
 * the user must drag the sheet fully off-screen to dismiss, which gives the ~80%-drag
 * threshold behaviour described in the design spec.
 *
 * @param personId      The person whose debts are being settled.
 * @param personName    Shown in the sheet header.
 * @param isOwedToMe    Which direction of debts to settle.
 * @param currency      Currency for display (default EUR).
 * @param onDismiss     Called when the sheet is closed without settlement.
 * @param onSuccess     Called after a successful settlement with the snackbar message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementSheet(
    personId: Long,
    personName: String,
    isOwedToMe: Boolean,
    currency: String = "EUR",
    onDismiss: () -> Unit,
    onSuccess: (snackbarMessage: String) -> Unit,
    viewModel: SettlementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val openDebts by viewModel.openDebts.collectAsStateWithLifecycle()
    // skipPartiallyExpanded = true: only EXPANDED and HIDDEN states exist.
    // The sheet must be dragged fully off-screen to dismiss (~80-100% of its height),
    // which is the desired drag-threshold behaviour without any confirmValueChange restriction.
    val sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var amountInput by rememberSaveable { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Load open debts when sheet opens
    LaunchedEffect(personId, isOwedToMe) {
        viewModel.loadOpenDebts(personId, isOwedToMe)
    }

    // Update live preview whenever the amount input changes
    LaunchedEffect(amountInput) {
        val parsed = parseAmountInput(amountInput)
        viewModel.updatePreview(parsed)
    }

    // React to Success state — close sheet and notify caller
    LaunchedEffect(uiState) {
        if (uiState is SettlementUiState.Success) {
            val state = uiState as SettlementUiState.Success
            val message = formatAmount(state.result.totalSettled, currency)
            onSuccess(message)
        }
    }

    if (showConfirmDialog) {
        val currentAmount = (uiState as? SettlementUiState.Preview)?.inputAmount
            ?: parseAmountInput(amountInput)
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    stringResource(R.string.settlement_confirm_dialog_title),
                    fontFamily = OswaldFontFamily,
                    color = BugListColors.Platinum
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.settlement_confirm_dialog_message,
                        formatAmount(currentAmount, currency)
                    ),
                    fontFamily = RobotoCondensedFontFamily,
                    color = BugListColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.settleDebts(personId, currentAmount, isOwedToMe)
                }) {
                    Text(
                        stringResource(R.string.action_confirm),
                        color = BugListColors.Gold,
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = BugListColors.Muted
                    )
                }
            },
            containerColor = BugListColors.Surface
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BugListColors.Surface
    ) {
        when (val state = uiState) {
            is SettlementUiState.Processing -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BugListColors.Gold)
                }
            }

            is SettlementUiState.Success -> {
                SettlementSuccessContent(
                    result = state.result,
                    currency = currency,
                    onDone = onDismiss
                )
            }

            is SettlementUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 14.sp,
                        color = BugListColors.DebtRed,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    GoldButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            is SettlementUiState.Idle -> {
                SettlementInputContent(
                    state = state,
                    personName = personName,
                    isOwedToMe = isOwedToMe,
                    currency = currency,
                    amountInput = amountInput,
                    onAmountInputChange = { newInput -> amountInput = newInput },
                    previewItems = emptyList(),
                    onConfirm = { showConfirmDialog = true },
                    openDebts = openDebts
                )
            }

            is SettlementUiState.Preview -> {
                SettlementInputContent(
                    state = null,
                    personName = personName,
                    isOwedToMe = isOwedToMe,
                    currency = currency,
                    amountInput = amountInput,
                    onAmountInputChange = { newInput -> amountInput = newInput },
                    previewItems = state.preview,
                    onConfirm = { showConfirmDialog = true },
                    totalOpen = state.totalOpen,
                    openCount = state.openCount,
                    openDebts = openDebts
                )
            }
        }
    }
}

@Composable
private fun SettlementInputContent(
    state: SettlementUiState.Idle?,
    personName: String,
    isOwedToMe: Boolean,
    currency: String,
    amountInput: String,
    onAmountInputChange: (String) -> Unit,
    previewItems: List<SettlementPreviewItem>,
    onConfirm: () -> Unit,
    totalOpen: Double = state?.totalOpen ?: 0.0,
    openCount: Int = state?.openCount ?: 0,
    openDebts: List<com.buglist.domain.model.DebtEntryWithPayments> = emptyList()
) {
    val scrollState = rememberScrollState()
    val hasValidAmount = parseAmountInput(amountInput) > 0.001

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        // Header — title left, quick-confirm button right (Gold background, black text)
        val isConfirmEnabled = hasValidAmount && totalOpen > 0.001
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settlement_title),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = BugListColors.Gold
                )
                Text(
                    text = personName,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Muted
                )
            }
            // Quick-confirm button: Gold background, black text, same shape as bottom button,
            // slightly smaller height so it fits the header row without crowding the title.
            Button(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BugListColors.Gold,
                    contentColor = androidx.compose.ui.graphics.Color.Black,
                    disabledContainerColor = BugListColors.GoldDim,
                    disabledContentColor = BugListColors.Muted
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(R.string.settlement_button_label),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Direction label
        val directionLabel = if (isOwedToMe) {
            stringResource(R.string.settlement_direction_owed_to_me)
        } else {
            stringResource(R.string.settlement_direction_i_owe)
        }
        Text(
            text = directionLabel,
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = BugListColors.Gold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(4.dp))

        // Summary row — entries count left, total amount right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settlement_open_entries, openCount),
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 13.sp,
                color = BugListColors.Muted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatAmount(totalOpen, currency),
                fontFamily = BebasNeueFontFamily,
                fontSize = 24.sp,
                color = if (isOwedToMe) BugListColors.DebtGreen else BugListColors.DebtRed
            )
        }

        Spacer(Modifier.height(8.dp))

        // Expandable list of open debt entries
        var debtsExpanded by rememberSaveable { mutableStateOf(false) }

        Surface(
            onClick = { debtsExpanded = !debtsExpanded },
            color = BugListColors.SurfaceHigh,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settlement_open_list_title),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BugListColors.Platinum,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (debtsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BugListColors.Gold
                    )
                }

                if (debtsExpanded && openDebts.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = BugListColors.Divider
                    )
                    openDebts.forEachIndexed { index, debtWithPayments ->
                        val dateStr = remember(debtWithPayments.entry.date) {
                            SimpleDateFormat("dd.MM.yy", Locale.GERMAN)
                                .format(Date(debtWithPayments.entry.date))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debtWithPayments.entry.description ?: dateStr,
                                    fontFamily = RobotoCondensedFontFamily,
                                    fontSize = 13.sp,
                                    color = BugListColors.Platinum
                                )
                                if (debtWithPayments.entry.description != null) {
                                    Text(
                                        text = dateStr,
                                        fontFamily = RobotoCondensedFontFamily,
                                        fontSize = 11.sp,
                                        color = BugListColors.Muted
                                    )
                                }
                                if (debtWithPayments.entry.tags.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    SettlementTagRow(tags = debtWithPayments.entry.tags)
                                }
                            }
                            Text(
                                text = formatAmount(debtWithPayments.remaining, currency),
                                fontFamily = BebasNeueFontFamily,
                                fontSize = 16.sp,
                                color = BugListColors.Gold
                            )
                        }
                        if (index < openDebts.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = BugListColors.Divider
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Amount input pad
        AmountInputPad(
            inputString = amountInput,
            onInputChange = onAmountInputChange,
            maxValue = 999_999.99,
            currency = currency,
            modifier = Modifier.fillMaxWidth()
        )

        // Preview section — only shown when there's a valid amount
        if (previewItems.isNotEmpty() && hasValidAmount) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settlement_preview_title),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BugListColors.Platinum,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(BugListColors.SurfaceHigh, RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column {
                    previewItems.forEachIndexed { index, item ->
                        SettlementPreviewRow(item = item, currency = currency)
                        if (index < previewItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = BugListColors.Divider
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bottom confirm button — full-width Gold, same shape as header quick-confirm button
        GoldButton(
            text = stringResource(R.string.settlement_confirm_button),
            onClick = onConfirm,
            enabled = hasValidAmount && totalOpen > 0.001,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SettlementPreviewRow(
    item: SettlementPreviewItem,
    currency: String
) {
    val dateStr = remember(item.entry.date) {
        SimpleDateFormat("dd.MM.yy", Locale.GERMAN).format(Date(item.entry.date))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        val (indicatorText, indicatorColor) = when {
            !item.isTouched -> "  " to BugListColors.Muted
            item.resultingStatus == DebtStatus.PAID -> "✓" to BugListColors.DebtGreen
            else -> "~" to BugListColors.Gold
        }

        Text(
            text = indicatorText,
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = indicatorColor,
            modifier = Modifier.width(20.dp)
        )

        Spacer(Modifier.width(8.dp))

        // Entry description / date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.entry.description ?: dateStr,
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 13.sp,
                color = BugListColors.Platinum
            )
            Text(
                text = dateStr,
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 11.sp,
                color = BugListColors.Muted
            )
            if (item.entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                SettlementTagRow(tags = item.entry.tags)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Amount paid in this settlement (if touched)
        if (item.isTouched) {
            Text(
                text = formatAmount(item.amountToPay, currency),
                fontFamily = BebasNeueFontFamily,
                fontSize = 16.sp,
                color = BugListColors.Gold
            )
            Spacer(Modifier.width(8.dp))
        }

        // Result status label
        val (statusLabel, statusColor) = when {
            !item.isTouched -> stringResource(R.string.settlement_preview_open) to BugListColors.Muted
            item.resultingStatus == DebtStatus.PAID ->
                stringResource(R.string.settlement_preview_paid) to BugListColors.DebtGreen
            else -> stringResource(R.string.settlement_preview_partial) to BugListColors.Gold
        }

        Text(
            text = statusLabel,
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = statusColor
        )
    }
}

@Composable
private fun SettlementSuccessContent(
    result: com.buglist.domain.model.SettlementResult,
    currency: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = BugListColors.DebtGreen,
            modifier = Modifier.size(56.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.settlement_success_title),
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BugListColors.DebtGreen
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${formatAmount(result.totalSettled, currency)} ${stringResource(R.string.settlement_success_settled)}",
            fontFamily = BebasNeueFontFamily,
            fontSize = 28.sp,
            color = BugListColors.Gold
        )

        Spacer(Modifier.height(16.dp))

        // Fully settled entries
        result.settledEntries.forEach { settled ->
            val dateStr = SimpleDateFormat("dd.MM", Locale.GERMAN).format(Date(settled.debtEntry.date))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓ ", fontFamily = OswaldFontFamily, fontSize = 14.sp, color = BugListColors.DebtGreen)
                Text(
                    text = stringResource(R.string.settlement_success_entry_paid, dateStr),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Platinum,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Partial entry (if any)
        result.partialEntry?.let { partial ->
            val dateStr = SimpleDateFormat("dd.MM", Locale.GERMAN).format(Date(partial.debtEntry.date))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u25d1 ",
                    fontFamily = OswaldFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Gold
                )
                Text(
                    text = stringResource(
                        R.string.settlement_success_entry_partial,
                        dateStr,
                        formatAmount(
                            maxOf(0.0, partial.debtEntry.amount - partial.amountPaid),
                            currency
                        )
                    ),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Platinum,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        GoldButton(
            text = stringResource(R.string.settlement_success_done),
            onClick = onDone
        )

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Renders a compact row of tag chips, wrapping to multiple lines if needed.
 * Used in both the expandable open-debts list and the settlement preview rows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettlementTagRow(tags: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .background(
                        color = BugListColors.Gold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = tag,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 10.sp,
                    color = BugListColors.Gold
                )
            }
        }
    }
}

/**
 * Parses a German-locale amount string (e.g. "123,45") to a [Double].
 * Returns 0.0 if the string is empty or unparseable.
 */
private fun parseAmountInput(input: String): Double {
    if (input.isBlank()) return 0.0
    return try {
        input.replace(",", ".").toDouble()
    } catch (e: NumberFormatException) {
        0.0
    }
}

/**
 * Formats a [Double] amount as a German-locale input string for the AmountInputPad.
 * Integer values are returned without decimals (e.g. 100.0 → "100").
 */
private fun formatAmountForInput(value: Double): String {
    return if (value == kotlin.math.floor(value)) {
        value.toLong().toString()
    } else {
        String.format(Locale.ROOT, "%.2f", value).replace(".", ",")
    }
}
