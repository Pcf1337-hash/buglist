package com.buglist.presentation.person_detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Person
import com.buglist.presentation.add_debt.AddDebtSheet
import com.buglist.presentation.add_debt.AddPaymentSheet
import com.buglist.presentation.components.AmountText
import com.buglist.presentation.edit_person.EditPersonSheet
import com.buglist.presentation.components.DebtCard
import com.buglist.presentation.components.PersonAvatar
import com.buglist.presentation.settlement.SettlementSheet
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.BebasNeueFontFamily
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
    var showEditPerson by remember { mutableStateOf(false) }
    var showAddDebt by remember { mutableStateOf(false) }
    var paymentDebtId by remember { mutableStateOf<Long?>(null) }
    // Edit sheet: non-null when editing an existing debt entry via long-press
    var editDebtEntry by remember { mutableStateOf<DebtEntry?>(null) }
    // Settlement sheet: null = closed, true = settling "owed to me", false = settling "I owe"
    var settlementDirection by remember { mutableStateOf<Boolean?>(null) }
    // Easter egg: kiss emoji for person named "Nos"
    var showKissEgg by remember { mutableStateOf(false) }
    // Tracks which debt entry's context menu is open (null = closed)
    var contextMenuDebtId by remember { mutableStateOf<Long?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val cancelledLabel = stringResource(R.string.person_detail_debt_cancelled)
    val undoLabel = stringResource(R.string.action_undo)

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
                            IconButton(onClick = { showEditPerson = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Person bearbeiten",
                                    tint = BugListColors.Platinum
                                )
                            }
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
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = false
                        viewModel.refresh()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        PersonDetailHeader(
                            person = state.person,
                            debts = state.debts,
                            activeTab = state.activeTab,
                            hasOpenDebtsOwedToMe = state.hasOpenDebtsOwedToMe,
                            hasOpenDebtsIOwe = state.hasOpenDebtsIOwe,
                            onSettleOwedToMe = { settlementDirection = true },
                            onSettleIOwe = { settlementDirection = false },
                            onLongPressBalance = {
                                // Copy just the net balance to clipboard
                                val balance = buildBalanceText(state.debts)
                                clipboardManager.setText(AnnotatedString(balance))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Betrag kopiert ✓")
                                }
                            },
                            onDoubleTapBalance = {
                                // Copy full formatted tab history — ready to paste in any messenger
                                val history = buildShareText(
                                    personName = state.person.name,
                                    activeTab = state.activeTab,
                                    debts = state.debts
                                )
                                clipboardManager.setText(AnnotatedString(history))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Historie kopiert – bereit zum Teilen ✓")
                                }
                            }
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
                            Box {
                                SwipeableDebtCard(
                                    debtWithPayments = debtWithPayments,
                                    expanded = state.expandedDebtId == debtWithPayments.entry.id,
                                    onTap = { viewModel.toggleDebtExpanded(debtWithPayments.entry.id) },
                                    // Long press → context menu (Bearbeiten / Stornieren)
                                    onContextMenu = { contextMenuDebtId = debtWithPayments.entry.id },
                                    onSwipeLeft = { paymentDebtId = debtWithPayments.entry.id },
                                    // Swipe right → open edit sheet directly
                                    onSwipeRight = { editDebtEntry = debtWithPayments.entry },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                                // Context menu — anchored to this card, opens on long press
                                DropdownMenu(
                                    expanded = contextMenuDebtId == debtWithPayments.entry.id,
                                    onDismissRequest = { contextMenuDebtId = null },
                                    containerColor = BugListColors.Surface
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.context_menu_edit),
                                                fontFamily = OswaldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = BugListColors.Gold
                                            )
                                        },
                                        onClick = {
                                            contextMenuDebtId = null
                                            editDebtEntry = debtWithPayments.entry
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.context_menu_cancel),
                                                fontFamily = OswaldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = BugListColors.DebtRed
                                            )
                                        },
                                        onClick = {
                                            contextMenuDebtId = null
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
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.context_menu_delete),
                                                fontFamily = OswaldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = BugListColors.Muted
                                            )
                                        },
                                        onClick = {
                                            contextMenuDebtId = null
                                            viewModel.deleteDebtEntry(debtWithPayments.entry.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                } // end PullToRefreshBox
            }

            // AddDebt sheet
            if (showAddDebt) {
                AddDebtSheet(
                    personId = viewModel.personId,
                    onDismiss = { showAddDebt = false },
                    onSaved = {
                        showAddDebt = false
                        viewModel.refresh()
                        if (state.person.name.lowercase() == "nos") showKissEgg = true
                    }
                )
            }

            // Edit debt sheet — opened via 2-second long press on a DebtCard
            val currentEditDebt = editDebtEntry
            if (currentEditDebt != null) {
                AddDebtSheet(
                    personId = currentEditDebt.personId,
                    existingDebt = currentEditDebt,
                    onDismiss = { editDebtEntry = null },
                    onSaved = {
                        editDebtEntry = null
                        viewModel.refresh()
                    }
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
                        onSaved = {
                            paymentDebtId = null
                            if (state.person.name.lowercase() == "nos") showKissEgg = true
                        }
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
                    onSuccess = { _ ->
                        settlementDirection = null
                        if (state.person.name.lowercase() == "nos") showKissEgg = true
                    }
                )
            }

            // Edit person sheet — opened via the pencil icon in the TopAppBar
            if (showEditPerson) {
                EditPersonSheet(
                    person = state.person,
                    onDismiss = { showEditPerson = false },
                    onSaved = { showEditPerson = false }
                )
            }

            // Easter egg overlay — kiss emoji for person named "Nos"
            if (showKissEgg) {
                KissEggOverlay(onFinished = { showKissEgg = false })
            }
        }
    }
}

/**
 * Full-screen overlay that fades in the kiss emoji (💋), holds briefly, then fades out.
 * Total duration: ~1500ms. Calls [onFinished] when the animation completes.
 *
 * Used as an Easter Egg when a debt action is performed for a person named "Nos".
 */
@Composable
private fun KissEggOverlay(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in: 400ms
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 400))
        // Hold: 700ms
        kotlinx.coroutines.delay(700L)
        // Fade out: 400ms
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 400))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uD83D\uDC8B",
            fontSize = 120.sp,
            fontFamily = BebasNeueFontFamily
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonDetailHeader(
    person: Person,
    debts: List<DebtEntryWithPayments>,
    activeTab: DebtTab,
    hasOpenDebtsOwedToMe: Boolean,
    hasOpenDebtsIOwe: Boolean,
    onSettleOwedToMe: () -> Unit,
    onSettleIOwe: () -> Unit,
    onLongPressBalance: () -> Unit,
    onDoubleTapBalance: () -> Unit
) {
    // Balance calculation per status:
    // OPEN/PARTIAL → remaining (what's still outstanding)
    // PAID         → entry.amount (original debt, totalPaid may be 0 for direct-mark-as-paid)
    // CANCELLED    → 0 (irrelevant)
    val netBalance = debts.sumOf { dwp ->
        val amount = when (dwp.entry.status) {
            DebtStatus.OPEN, DebtStatus.PARTIAL -> dwp.remaining
            DebtStatus.PAID -> dwp.entry.amount
            DebtStatus.CANCELLED -> 0.0
        }
        if (dwp.entry.isOwedToMe) amount else -amount
    }
    val hasAnyOpenDebts = hasOpenDebtsOwedToMe || hasOpenDebtsIOwe

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(BugListColors.Surface)
            .padding(24.dp)
    ) {
        PersonAvatar(
            name = person.name,
            avatarColor = person.avatarColor,
            size = 72.dp,
            avatarImagePath = person.avatarImagePath
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = person.name.uppercase(),
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = BugListColors.Platinum
        )
        Spacer(Modifier.height(8.dp))
        // Long press → copy balance | Double tap → copy full tab history
        AmountText(
            amount = netBalance,
            fontSize = 40.sp,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onLongPressBalance,
                onDoubleClick = onDoubleTapBalance
            )
        )
        Text(
            text = "Gedrückt halten = Summe  •  2× tippen = Detaillierte Liste",
            fontFamily = RobotoCondensedFontFamily,
            fontSize = 10.sp,
            color = BugListColors.Muted.copy(alpha = 0.6f)
        )

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

/**
 * Debt card with swipe gestures and a standard long-press context menu.
 *
 * Gestures:
 * - Tap           → expand/collapse ([onTap])
 * - Swipe right   → open edit sheet ([onSwipeRight])
 * - Swipe left    → open partial payment sheet ([onSwipeLeft])
 * - Long press    → system-standard duration (~500 ms), triggers [onContextMenu]
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeableDebtCard(
    debtWithPayments: DebtEntryWithPayments,
    expanded: Boolean,
    onTap: () -> Unit,
    onContextMenu: () -> Unit,
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
        },
        // Require 60% of the item width to be swiped before triggering —
        // prevents accidental activation from casual scrolling touches.
        positionalThreshold = { totalDistance -> totalDistance * 0.60f }
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
                // Swipe right → edit (Gold)
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Text(
                        stringResource(R.string.person_detail_swipe_edit),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BugListColors.Gold
                    )
                }
                Spacer(Modifier.weight(1f))
                // Swipe left → partial payment (Orange)
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
        // Pass onClick = null so DebtCard doesn't add its own clickable.
        // combinedClickable handles both tap and long-press with system-standard timing.
        DebtCard(
            debtWithPayments = debtWithPayments,
            expanded = expanded,
            onClick = null,
            modifier = Modifier.combinedClickable(
                onClick = onTap,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onContextMenu()
                }
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Clipboard / Share helpers
// ---------------------------------------------------------------------------

/**
 * Returns the net balance of [debts] as a plain-text string suitable for copying.
 * Example: "+€ 150,00" or "-€ 42,50"
 */
private fun buildBalanceText(debts: List<DebtEntryWithPayments>): String {
    val net = debts.sumOf { dwp ->
        val amount = when (dwp.entry.status) {
            DebtStatus.OPEN, DebtStatus.PARTIAL -> dwp.remaining
            DebtStatus.PAID -> dwp.entry.amount
            DebtStatus.CANCELLED -> 0.0
        }
        if (dwp.entry.isOwedToMe) amount else -amount
    }
    val currency = debts.firstOrNull()?.entry?.currency ?: "EUR"
    val symbol = currencySymbol(currency)
    val sign = if (net >= 0) "+" else ""
    return "$sign$symbol ${String.format(Locale.GERMAN, "%.2f", kotlin.math.abs(net))}"
}

/**
 * Builds a messenger-ready formatted debt history for [personName] and the current [activeTab].
 *
 * Format example:
 * ```
 * Schulden mit MIKE – Offen
 *
 * 01.03.26  Poker Night       +€ 50,00
 * 15.02.26  Drinks            -€ 20,00
 * 10.02.26  —                +€ 100,00
 *
 * Gesamt: +€ 130,00
 * ```
 */
/**
 * Builds a WhatsApp-friendly share text for the current tab's debt list.
 *
 * Design rules:
 *  - No space-based column alignment (proportional font in messengers breaks it).
 *  - Each entry on its own line: `• DD.MM.YY | [sign][symbol] [amount]`
 *  - Description and tags appended inline after ` · ` when present.
 *  - Short enough per line that even a narrow phone won't wrap mid-entry.
 */
private fun buildShareText(
    personName: String,
    activeTab: DebtTab,
    debts: List<DebtEntryWithPayments>
): String {
    val tabLabel = when (activeTab) {
        DebtTab.OPEN -> "Offen"
        DebtTab.PAID -> "Bezahlt"
        DebtTab.ALL  -> "Alle"
    }
    val currency = debts.firstOrNull()?.entry?.currency ?: "EUR"
    val symbol = currencySymbol(currency)
    val df = SimpleDateFormat("dd.MM.yy", Locale.GERMAN)

    return buildString {
        appendLine("Schulden mit ${personName.uppercase()} – $tabLabel")
        appendLine()
        debts.forEach { dwp ->
            val date = df.format(Date(dwp.entry.date))
            val raw = when (dwp.entry.status) {
                DebtStatus.OPEN, DebtStatus.PARTIAL -> dwp.remaining
                DebtStatus.PAID                     -> dwp.entry.amount
                DebtStatus.CANCELLED                -> 0.0
            }
            val signed = if (dwp.entry.isOwedToMe) raw else -raw
            val sign = if (signed >= 0) "+" else "-"
            val amount = "$sign$symbol ${String.format(Locale.GERMAN, "%.2f", kotlin.math.abs(signed))}"

            // Optional description and tags — appended after a middle-dot separator
            val desc = dwp.entry.description?.trim()?.takeIf { it.isNotBlank() }
            val tags = dwp.entry.tags.takeIf { it.isNotEmpty() }?.joinToString(", ")

            val meta = buildString {
                if (desc != null) append(" · $desc")
                if (tags != null) append(" [$tags]")
            }

            appendLine("• $date | $amount$meta")
        }
        appendLine()
        append("Gesamt: ${buildBalanceText(debts)}")
    }
}

/** Maps ISO currency code to display symbol. Falls back to the code itself. */
private fun currencySymbol(code: String): String = when (code.uppercase()) {
    "EUR" -> "€"
    "USD" -> "$"
    "GBP" -> "£"
    "CHF" -> "CHF"
    else  -> code
}
