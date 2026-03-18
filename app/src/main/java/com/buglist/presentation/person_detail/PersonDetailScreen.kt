package com.buglist.presentation.person_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.Person
import com.buglist.presentation.add_debt.AddDebtSheet
import com.buglist.presentation.add_debt.AddPaymentSheet
import com.buglist.presentation.components.AmountText
import com.buglist.presentation.components.DebtCard
import com.buglist.presentation.components.PersonAvatar
import com.buglist.presentation.settlement.SettlementSheet
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import kotlinx.coroutines.launch

/**
 * Person detail screen — shows all debts for a specific person.
 *
 * Features:
 * - Tab filter: OFFEN / BEZAHLT / ALLE
 * - Expandable DebtCards with payment history
 * - Swipe-left to open partial payment sheet
 * - Swipe-right to cancel a debt (with Undo snackbar)
 * - FAB to add new debt
 * - Delete-person confirmation dialog
 *
 * @param onBack Navigate back to dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    onBack: () -> Unit,
    viewModel: PersonDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddDebt by remember { mutableStateOf(false) }
    var paymentDebtId by remember { mutableStateOf<Long?>(null) }
    // Edit sheet: non-null when editing an existing debt entry via long-press
    var editDebtEntry by remember { mutableStateOf<DebtEntry?>(null) }
    // Settlement sheet: null = closed, true = settling "owed to me", false = settling "I owe"
    var settlementDirection by remember { mutableStateOf<Boolean?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cancelledLabel = stringResource(R.string.person_detail_debt_cancelled)
    val undoLabel = stringResource(R.string.action_undo)
    val settlementSuccessPrefix = stringResource(R.string.settlement_snackbar_success)

    when (val state = uiState) {
        is PersonDetailUiState.Loading -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BugListColors.Background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BugListColors.Gold)
            }
        }
        is PersonDetailUiState.PersonNotFound -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BugListColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.person_not_found),
                    fontFamily = OswaldFontFamily,
                    fontSize = 18.sp,
                    color = BugListColors.Muted
                )
            }
        }
        is PersonDetailUiState.Ready -> {
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Text(
                            stringResource(R.string.person_detail_delete_title),
                            fontFamily = OswaldFontFamily,
                            color = BugListColors.Platinum
                        )
                    },
                    text = {
                        Text(
                            stringResource(R.string.person_detail_delete_message, state.person.name),
                            fontFamily = RobotoCondensedFontFamily,
                            color = BugListColors.Muted
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deletePerson()
                            showDeleteConfirm = false
                            onBack()
                        }) {
                            Text(
                                stringResource(R.string.action_delete),
                                color = BugListColors.DebtRed
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(
                                stringResource(R.string.action_cancel),
                                color = BugListColors.Muted
                            )
                        }
                    },
                    containerColor = BugListColors.Surface
                )
            }

            Scaffold(
                containerColor = BugListColors.Background,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                topBar = {
                    TopAppBar(
                        title = { Text("") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                    tint = BugListColors.Platinum
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = BugListColors.DebtRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = BugListColors.Background
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDebt = true },
                        containerColor = BugListColors.Gold,
                        contentColor = BugListColors.Background
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.person_detail_add_debt)
                        )
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    item {
                        PersonDetailHeader(
                            person = state.person,
                            debts = state.debts,
                            hasOpenDebtsOwedToMe = state.hasOpenDebtsOwedToMe,
                            hasOpenDebtsIOwe = state.hasOpenDebtsIOwe,
                            onSettleOwedToMe = { settlementDirection = true },
                            onSettleIOwe = { settlementDirection = false }
                        )
                    }
                    item {
                        DebtTabRow(
                            activeTab = state.activeTab,
                            onTabChange = viewModel::setActiveTab
                        )
                    }
                    if (state.debts.isEmpty()) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.person_detail_empty),
                                    fontFamily = OswaldFontFamily,
                                    fontSize = 18.sp,
                                    color = BugListColors.Muted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(state.debts, key = { it.entry.id }) { debtWithPayments ->
                            SwipeableDebtCard(
                                debtWithPayments = debtWithPayments,
                                expanded = state.expandedDebtId == debtWithPayments.entry.id,
                                onTap = { viewModel.toggleDebtExpanded(debtWithPayments.entry.id) },
                                onLongPress = { editDebtEntry = debtWithPayments.entry },
                                onSwipeLeft = { paymentDebtId = debtWithPayments.entry.id },
                                onSwipeRight = {
                                    val debtId = debtWithPayments.entry.id
                                    viewModel.cancelDebt(debtId)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = cancelledLabel,
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoCancelDebt(debtId)
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // AddDebt sheet
            if (showAddDebt) {
                AddDebtSheet(
                    personId = viewModel.personId,
                    onDismiss = { showAddDebt = false },
                    onSaved = { showAddDebt = false }
                )
            }

            // Edit debt sheet — opened via 2-second long press on a DebtCard
            val currentEditDebt = editDebtEntry
            if (currentEditDebt != null) {
                AddDebtSheet(
                    personId = currentEditDebt.personId,
                    existingDebt = currentEditDebt,
                    onDismiss = { editDebtEntry = null },
                    onSaved = { editDebtEntry = null }
                )
            }

            // AddPayment sheet
            val currentPaymentDebtId = paymentDebtId
            if (currentPaymentDebtId != null) {
                val debtWithPayments = state.debts.firstOrNull { it.entry.id == currentPaymentDebtId }
                if (debtWithPayments != null) {
                    AddPaymentSheet(
                        debtEntryId = currentPaymentDebtId,
                        remaining = debtWithPayments.remaining,
                        personName = state.person.name,
                        isOwedToMe = debtWithPayments.entry.isOwedToMe,
                        currency = debtWithPayments.entry.currency,
                        onDismiss = { paymentDebtId = null },
                        onSaved = { paymentDebtId = null }
                    )
                }
            }

            // Settlement sheet
            val currentSettlementDirection = settlementDirection
            if (currentSettlementDirection != null) {
                SettlementSheet(
                    personId = viewModel.personId,
                    personName = state.person.name,
                    isOwedToMe = currentSettlementDirection,
                    onDismiss = { settlementDirection = null },
                    onSuccess = { settledAmountFormatted ->
                        settlementDirection = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = String.format(settlementSuccessPrefix, settledAmountFormatted)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PersonDetailHeader(
    person: Person,
    debts: List<DebtEntryWithPayments>,
    hasOpenDebtsOwedToMe: Boolean,
    hasOpenDebtsIOwe: Boolean,
    onSettleOwedToMe: () -> Unit,
    onSettleIOwe: () -> Unit
) {
    val netBalance = debts.sumOf { dwp ->
        if (dwp.entry.isOwedToMe) dwp.remaining else -dwp.remaining
    }
    val hasAnyOpenDebts = hasOpenDebtsOwedToMe || hasOpenDebtsIOwe

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(BugListColors.Surface)
            .padding(24.dp)
    ) {
        PersonAvatar(name = person.name, avatarColor = person.avatarColor, size = 72.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = person.name.uppercase(),
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = BugListColors.Platinum
        )
        Spacer(Modifier.height(8.dp))
        AmountText(amount = -netBalance, fontSize = 40.sp)

        // TILGEN button(s) — only shown when there are open/partial debts
        if (hasAnyOpenDebts) {
            Spacer(Modifier.height(16.dp))
            if (hasOpenDebtsOwedToMe && hasOpenDebtsIOwe) {
                // Both directions have open debts — show two smaller buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSettleOwedToMe,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BugListColors.DebtGreen,
                            contentColor = BugListColors.Background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.settlement_button_label),
                            fontFamily = OswaldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = onSettleIOwe,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BugListColors.DebtRed,
                            contentColor = BugListColors.Platinum
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.settlement_button_label),
                            fontFamily = OswaldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            } else {
                // Single direction — full-width button, color depends on direction
                val onClick = if (hasOpenDebtsOwedToMe) onSettleOwedToMe else onSettleIOwe
                val buttonColor = if (hasOpenDebtsOwedToMe) BugListColors.DebtGreen else BugListColors.DebtRed
                val textColor = if (hasOpenDebtsOwedToMe) BugListColors.Background else BugListColors.Platinum
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.settlement_button_label),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtTabRow(activeTab: DebtTab, onTabChange: (DebtTab) -> Unit) {
    val tabs = listOf(
        DebtTab.OPEN to stringResource(R.string.person_detail_tab_open),
        DebtTab.PAID to stringResource(R.string.person_detail_tab_paid),
        DebtTab.ALL to stringResource(R.string.person_detail_tab_all)
    )
    val selectedIndex = tabs.indexOfFirst { it.first == activeTab }

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = BugListColors.Surface,
        contentColor = BugListColors.Gold,
        indicator = { tabPositions ->
            SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = BugListColors.Gold
            )
        }
    ) {
        tabs.forEachIndexed { index, (tab, label) ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabChange(tab) },
                text = {
                    Text(
                        text = label,
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (selectedIndex == index) BugListColors.Gold else BugListColors.Muted
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDebtCard(
    debtWithPayments: DebtEntryWithPayments,
    expanded: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onSwipeRight(); false }
                SwipeToDismissBoxValue.EndToStart -> { onSwipeLeft(); false }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Text(
                        stringResource(R.string.person_detail_swipe_delete),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BugListColors.DebtRed
                    )
                }
                Spacer(Modifier.weight(1f))
                if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Text(
                        stringResource(R.string.person_detail_swipe_partial_payment),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BugListColors.Orange
                    )
                }
            }
        },
        modifier = modifier
    ) {
        // 2-second long press opens edit mode.
        // pointerInput coroutine scope is NOT restricted, so we can use launch{} freely.
        // For each finger-down: start a 2000ms delay job. Monitor pointer events in
        // a loop; if the finger lifts before 2000ms, cancel the delay. When the delay
        // fires while the finger is still down: haptic + open edit sheet.
        DebtCard(
            debtWithPayments = debtWithPayments,
            expanded = expanded,
            onClick = onTap,
            modifier = Modifier.pointerInput(debtWithPayments.entry.id) {
                while (true) {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var delayJob: kotlinx.coroutines.Job? = null
                        delayJob = kotlinx.coroutines.MainScope().launch {
                            kotlinx.coroutines.delay(2_000L)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                        // Monitor until finger lifts
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || change.changedToUp()) {
                                delayJob?.cancel()
                                break
                            }
                        }
                    }
                }
            }
        )
    }
}
