package com.buglist.presentation.add_debt

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.presentation.components.AmountInputPad
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.components.formatAmount
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet for recording a partial payment on an existing debt.
 *
 * Features:
 * - [isOwedToMe] controls the confirm button color:
 *   true  = person owes user (user receives money) → GREEN button
 *   false = user owes person (user pays out)       → RED button
 * - Double-booking protection: after a successful payment, pressing BUCHEN again
 *   without entering a new amount shows a Toast reminder and does NOT re-book.
 *
 * @param debtEntryId   The debt being (partially) paid.
 * @param remaining     The currently outstanding balance — used as max value for [AmountInputPad].
 * @param personName    Person name shown in the title.
 * @param isOwedToMe    Direction of the debt (affects confirm button color).
 * @param currency      Currency of the debt.
 * @param onDismiss     Called when the sheet is closed.
 * @param onSaved       Called after successful payment recording.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentSheet(
    debtEntryId: Long,
    remaining: Double,
    personName: String,
    isOwedToMe: Boolean,
    currency: String = "EUR",
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddDebtViewModel = hiltViewModel()
) {
    val uiState by viewModel.paymentUiState.collectAsStateWithLifecycle()
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var amountInput by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    // Date formatter for the last-payment toast
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)

    // Toast string resources captured outside the LaunchedEffect to avoid @Composable constraint
    val lastPaymentPrefix = stringResource(R.string.add_payment_last_payment_toast_prefix)

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddPaymentUiState.Success -> {
                viewModel.resetPaymentState()
                onSaved()
            }
            is AddPaymentUiState.ShowLastPaymentToast -> {
                // Show toast and transition back to Idle — do NOT close the sheet
                val formattedAmount = formatAmount(state.amount, currency)
                val formattedDate = dateFormat.format(Date(state.date))
                Toast.makeText(
                    context,
                    "$lastPaymentPrefix $formattedAmount am $formattedDate",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetPaymentState()
            }
            else -> Unit
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BugListColors.Surface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Header
            Text(
                text = stringResource(R.string.add_payment_title).uppercase(),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BugListColors.Gold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Text(
                text = "$personName – ${stringResource(R.string.add_payment_remaining)}: ${formatAmount(remaining, currency)}",
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 13.sp,
                color = BugListColors.Muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            Spacer(Modifier.height(4.dp))

            // Amount pad — max is remaining
            AmountInputPad(
                inputString = amountInput,
                onInputChange = { newInput ->
                    amountInput = newInput
                    viewModel.onAmountInputChanged()
                },
                maxValue = remaining,
                currency = currency,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // "Pay all" quick button
            GoldButton(
                text = stringResource(R.string.add_payment_pay_all),
                onClick = {
                    amountInput = formatAmountForInput(remaining)
                    viewModel.onAmountInputChanged()
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Note field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Text(
                        stringResource(R.string.add_payment_note_hint),
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

            if (uiState is AddPaymentUiState.Error) {
                Text(
                    text = (uiState as AddPaymentUiState.Error).message,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 13.sp,
                    color = BugListColors.DebtRed,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            // Confirm button: GREEN when person owes user (user receives), RED when user pays
            val buttonColor = if (isOwedToMe) BugListColors.DebtGreen else BugListColors.DebtRed
            val buttonTextColor = if (isOwedToMe) BugListColors.Background else BugListColors.Platinum
            Button(
                onClick = {
                    viewModel.savePayment(
                        debtEntryId = debtEntryId,
                        amountString = amountInput,
                        note = note
                    )
                },
                enabled = amountInput.isNotBlank() && amountInput != "0",
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = buttonTextColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_payment_book),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun formatAmountForInput(value: Double): String {
    return if (value == kotlin.math.floor(value)) {
        value.toLong().toString()
    } else {
        String.format(java.util.Locale.ROOT, "%.2f", value).replace(".", ",")
    }
}
