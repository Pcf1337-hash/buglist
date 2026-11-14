package com.buglist.presentation.settlement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.presentation.components.AmountInputPad
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.components.formatAmount
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import java.util.Locale

/**
 * Bottom sheet for settling (tilging) debts of a person against a total amount using FIFO.
 *
 * Features:
 * - Minimal amount entry with a shared keypad.
 * - Confirmation dialog before committing.
 * - Completion is a one-shot event after a successful booking.
 *
 * Swipe and scrim do not close the sheet. Close and system back do, without booking.
 *
 * @param personId      The person whose debts are being settled.
 * @param personName    Retained for callers identifying the person.
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
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountInput by rememberSaveable { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showOverpayDialog by remember { mutableStateOf(false) }

    // Decides whether to show the regular confirm dialog or the overpayment dialog.
    // Called from the confirm button below the amount display.
    val handleConfirm: () -> Unit = {
        val inputAmount = parseAmountInput(amountInput)
        val totalOpen = when (val s = uiState) {
            is SettlementUiState.Preview -> s.totalOpen
            is SettlementUiState.Idle -> s.totalOpen
            else -> 0.0
        }
        if (totalOpen > 0.001 && inputAmount > totalOpen + 0.005) {
            showOverpayDialog = true
        } else {
            showConfirmDialog = true
        }
    }

    // Load open debts when sheet opens
    LaunchedEffect(personId, isOwedToMe) {
        viewModel.loadOpenDebts(personId, isOwedToMe)
    }

    // Update live preview whenever the amount input changes
    LaunchedEffect(amountInput) {
        val parsed = parseAmountInput(amountInput)
        viewModel.updatePreview(parsed)
    }

    val currentOnSuccess by rememberUpdatedState(onSuccess)
    LaunchedEffect(viewModel, currency) {
        viewModel.completedSettlements.collect { result ->
            currentOnSuccess(formatAmount(result.totalSettled, currency))
        }
    }

    if (showOverpayDialog) {
        val inputAmount = (uiState as? SettlementUiState.Preview)?.inputAmount
            ?: parseAmountInput(amountInput)
        val totalOpen = when (val s = uiState) {
            is SettlementUiState.Preview -> s.totalOpen
            is SettlementUiState.Idle -> s.totalOpen
            else -> openDebts.sumOf { it.remaining }
        }
        val overpayAmount = inputAmount - totalOpen
        OverpaymentDialog(
            inputAmount = inputAmount,
            totalOpen = totalOpen,
            overpayAmount = overpayAmount,
            isOwedToMe = isOwedToMe,
            currency = currency,
            onDismiss = { showOverpayDialog = false },
            onTip = {
                showOverpayDialog = false
                viewModel.settleDebts(personId, inputAmount, isOwedToMe)
            },
            onCounterDebt = {
                showOverpayDialog = false
                viewModel.settleDebtsWithCounterDebt(personId, inputAmount, isOwedToMe, currency)
            }
        )
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
        sheetGesturesEnabled = false,
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = uiState !is SettlementUiState.Processing,
            shouldDismissOnClickOutside = false
        ),
        containerColor = BugListColors.Surface
    ) {
        when (val state = uiState) {
            is SettlementUiState.Processing, is SettlementUiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BugListColors.Gold)
                }
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
                    currency = currency,
                    amountInput = amountInput,
                    onAmountInputChange = { newInput -> amountInput = newInput },
                    onConfirm = handleConfirm,
                    onClose = onDismiss,
                    totalOpen = state.totalOpen
                )
            }

            is SettlementUiState.Preview -> {
                SettlementInputContent(
                    currency = currency,
                    amountInput = amountInput,
                    onAmountInputChange = { newInput -> amountInput = newInput },
                    onConfirm = handleConfirm,
                    onClose = onDismiss,
                    totalOpen = state.totalOpen
                )
            }
        }
    }
}

@Composable
private fun SettlementInputContent(
    currency: String,
    amountInput: String,
    onAmountInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    totalOpen: Double
) {
    val isConfirmEnabled = parseAmountInput(amountInput) > 0.001 && totalOpen > 0.001
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val keyHeight = ((maxHeight - 220.dp) / 4).coerceIn(48.dp, 64.dp)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close,
                        contentDescription = stringResource(R.string.settlement_close),
                        tint = BugListColors.Platinum)
                }
            }
            AmountInputPad(
                inputString = amountInput,
                onInputChange = onAmountInputChange,
                currency = currency,
                modifier = Modifier.fillMaxWidth(),
                keyHeight = keyHeight,
                belowDisplay = {
                    GoldButton(
                        text = stringResource(R.string.settlement_confirm_button),
                        emphasized = true,
                        onClick = onConfirm,
                        enabled = isConfirmEnabled
                    )
                }
            )
        }
    }
}

@Composable
private fun OverpaymentDialog(
    inputAmount: Double,
    totalOpen: Double,
    overpayAmount: Double,
    isOwedToMe: Boolean,
    currency: String,
    onDismiss: () -> Unit,
    onTip: () -> Unit,
    onCounterDebt: () -> Unit
) {
    // Direction label for the counter-debt entry
    val counterDebtDesc = if (isOwedToMe) {
        // person overpaid me → I now owe them
        stringResource(
            R.string.settlement_overpay_counter_debt_desc_owed_to_me,
            formatAmount(overpayAmount, currency)
        )
    } else {
        // I overpaid person → they now owe me
        stringResource(
            R.string.settlement_overpay_counter_debt_desc_i_owe,
            formatAmount(overpayAmount, currency)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = BugListColors.Surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.settlement_overpay_dialog_title),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = BugListColors.Gold
                )
                Spacer(Modifier.height(10.dp))

                // Info: how much over
                Text(
                    text = stringResource(
                        R.string.settlement_overpay_dialog_info,
                        formatAmount(inputAmount, currency),
                        formatAmount(overpayAmount, currency),
                        formatAmount(totalOpen, currency)
                    ),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Muted
                )
                Spacer(Modifier.height(6.dp))

                // Question
                Text(
                    text = stringResource(R.string.settlement_overpay_question),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.Platinum
                )
                Spacer(Modifier.height(20.dp))

                // ── GEGENSCHULD (primary action) ───────────────────────────────
                Button(
                    onClick = onCounterDebt,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BugListColors.Gold,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text(
                        text = stringResource(R.string.settlement_overpay_counter_debt),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = counterDebtDesc,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 11.sp,
                    color = BugListColors.Muted,
                    modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 12.dp)
                )

                // ── TRINKGELD (secondary action) ───────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = BugListColors.Gold.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    TextButton(
                        onClick = onTip,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settlement_overpay_tip),
                            fontFamily = OswaldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BugListColors.Platinum
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settlement_overpay_tip_desc),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 11.sp,
                    color = BugListColors.Muted,
                    modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 8.dp)
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
