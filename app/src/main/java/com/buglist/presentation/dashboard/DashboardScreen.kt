package com.buglist.presentation.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.data.remote.UpdateState
import com.buglist.domain.model.DashboardListItem
import com.buglist.domain.model.Divider
import com.buglist.domain.model.DividerLineStyle
import com.buglist.domain.model.PersonWithBalance
import com.buglist.presentation.components.AmountText
import com.buglist.presentation.components.PersonAvatar
import com.buglist.presentation.components.UpdateDialog
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Dashboard screen — shows all persons with their net balances and the global total.
 *
 * The crew list renders both [DashboardListItem.PersonItem] rows and
 * [DashboardListItem.DividerItem] separator rows interleaved by sort index.
 *
 * Supports manual drag-to-reorder via a pencil/edit icon next to the CREW header.
 * In edit mode, drag handles appear on each row; dividers additionally show a delete
 * button. Exiting edit mode persists the new order for both persons and dividers.
 *
 * @param onPersonClick Navigate to person detail screen.
 * @param onAddItem     Open the add-item choice sheet (person or divider).
 * @param onStatistics  Navigate to statistics screen.
 * @param onSettings    Navigate to settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onPersonClick: (Long) -> Unit,
    onAddItem: () -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    // Edit-mode state — local mutable copy used during reorder
    var editMode by remember { mutableStateOf(false) }
    var editableItems by remember { mutableStateOf<List<DashboardListItem>>(emptyList()) }

    // Reorderable LazyList state (always created to satisfy Compose key ordering rules)
    val reorderLazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(reorderLazyListState) { from, to ->
        editableItems = editableItems.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    if (updateState is UpdateState.UpdateAvailable) {
        val update = updateState as UpdateState.UpdateAvailable
        UpdateDialog(
            updateState = update,
            onUpdate = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
                context.startActivity(intent)
                viewModel.onUpdateDismissed()
            },
            onSkip = { viewModel.onUpdateSkipped(update.newVersion) },
            onDismiss = { viewModel.onUpdateDismissed() }
        )
    }

    Scaffold(
        containerColor = BugListColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = BugListColors.Gold
                    )
                },
                actions = {
                    if (!editMode) {
                        IconButton(onClick = onStatistics) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = stringResource(R.string.nav_statistics),
                                tint = BugListColors.Platinum
                            )
                        }
                        IconButton(onClick = onSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                tint = BugListColors.Platinum
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BugListColors.Background
                )
            )
        },
        floatingActionButton = {
            if (!editMode) {
                FloatingActionButton(
                    onClick = onAddItem,
                    containerColor = BugListColors.Gold,
                    contentColor = BugListColors.Background
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.dashboard_add_person)
                    )
                }
            }
        }
    ) { paddingValues ->

        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator(color = BugListColors.Gold)
                }
            }

            is DashboardUiState.Ready -> {
                val hasPersons = state.persons.isNotEmpty()

                if (!hasPersons && state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        DashboardEmptyState(onAddItem = onAddItem)
                    }
                } else if (editMode) {
                    // ── EDIT MODE ────────────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        DashboardSummaryHeader(
                            totalBalance = state.totalBalance,
                            totalOwedToMe = state.totalOwedToMe,
                            totalIOwe = state.totalIOwe
                        )
                        // CREW header with DONE button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_crew_header),
                                fontFamily = OswaldFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BugListColors.Gold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    viewModel.saveOrder(editableItems)
                                    editMode = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Fertig",
                                    tint = BugListColors.Gold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "FERTIG",
                                    fontFamily = OswaldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BugListColors.Gold
                                )
                            }
                        }
                        // Reorderable LazyColumn
                        LazyColumn(
                            state = reorderLazyListState,
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(editableItems, key = { it.listKey }) { item ->
                                ReorderableItem(reorderableState, key = item.listKey) { isDragging ->
                                    val shadowElevation by animateDpAsState(
                                        targetValue = if (isDragging) 8.dp else 0.dp,
                                        label = "drag_elevation"
                                    )
                                    Surface(
                                        shadowElevation = shadowElevation,
                                        color = if (isDragging) BugListColors.SurfaceHigh
                                        else BugListColors.Background
                                    ) {
                                        when (item) {
                                            is DashboardListItem.PersonItem ->
                                                EditablePersonRow(
                                                    personWithBalance = item.data,
                                                    dragHandleModifier = Modifier.draggableHandle()
                                                )
                                            is DashboardListItem.DividerItem ->
                                                EditableDividerRow(
                                                    divider = item.data,
                                                    dragHandleModifier = Modifier.draggableHandle(),
                                                    onDelete = {
                                                        // Remove from local list + delete from DB
                                                        editableItems = editableItems.filter {
                                                            it !is DashboardListItem.DividerItem ||
                                                                    it.data.id != item.data.id
                                                        }
                                                        viewModel.deleteDivider(item.data.id)
                                                    }
                                                )
                                        }
                                        HorizontalDivider(
                                            color = BugListColors.Divider,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── NORMAL MODE ──────────────────────────────────────────────────
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { isRefreshing = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                DashboardSummaryHeader(
                                    totalBalance = state.totalBalance,
                                    totalOwedToMe = state.totalOwedToMe,
                                    totalIOwe = state.totalIOwe
                                )
                            }
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 16.dp, end = 4.dp,
                                            top = 8.dp, bottom = 8.dp
                                        )
                                ) {
                                    Text(
                                        text = stringResource(R.string.dashboard_crew_header),
                                        fontFamily = OswaldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BugListColors.Muted,
                                        letterSpacing = 2.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            editableItems = state.items
                                            editMode = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Reihenfolge anpassen",
                                            tint = BugListColors.Muted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            items(state.items, key = { it.listKey }) { item ->
                                when (item) {
                                    is DashboardListItem.PersonItem -> {
                                        PersonCard(
                                            personWithBalance = item.data,
                                            onClick = { onPersonClick(item.data.person.id) }
                                        )
                                        HorizontalDivider(
                                            color = BugListColors.Divider,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                    is DashboardListItem.DividerItem -> {
                                        DividerRow(divider = item.data)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Crew-list row composables ─────────────────────────────────────────────────

@Composable
private fun EditablePersonRow(
    personWithBalance: PersonWithBalance,
    dragHandleModifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        PersonAvatar(
            name = personWithBalance.person.name,
            avatarColor = personWithBalance.person.avatarColor,
            size = 44.dp,
            avatarImagePath = personWithBalance.person.avatarImagePath
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = personWithBalance.person.name,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BugListColors.Platinum
            )
            if (personWithBalance.openCount > 0) {
                Text(
                    text = "${personWithBalance.openCount} ${stringResource(R.string.dashboard_open_debts)}",
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 12.sp,
                    color = BugListColors.Muted
                )
            }
        }
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Ziehen zum Sortieren",
            tint = BugListColors.Muted,
            modifier = dragHandleModifier.size(24.dp)
        )
    }
}

@Composable
private fun EditableDividerRow(
    divider: Divider,
    dragHandleModifier: Modifier,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        // Delete button
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.divider_delete_content_description),
                tint = BugListColors.DebtRed,
                modifier = Modifier.size(20.dp)
            )
        }
        // Divider preview fills available space
        Box(modifier = Modifier.weight(1f)) {
            DividerRow(divider = divider)
        }
        // Drag handle — modifier carries ReorderableItemScope.draggableHandle()
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Ziehen zum Sortieren",
            tint = BugListColors.Muted,
            modifier = dragHandleModifier
                .size(24.dp)
                .padding(end = 4.dp)
        )
    }
}

/**
 * Renders a [Divider] separator row as it appears in the crew list (and in the preview).
 *
 * Layout: [left line] – [LABEL TEXT] – [right line]
 * Line style follows [Divider.lineStyle]; color follows [Divider.color].
 *
 * Public so [com.buglist.presentation.add_divider.AddDividerSheet] can use it for preview.
 */
@Composable
fun DividerRow(divider: Divider) {
    val lineColor  = Color(divider.color)
    val strokeDp: Dp = when (divider.lineStyle) {
        DividerLineStyle.THICK -> 3.dp
        else                   -> 1.dp
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        DividerLine(
            color = lineColor,
            lineStyle = divider.lineStyle,
            strokeDp = strokeDp,
            modifier = Modifier.weight(1f)
        )
        if (divider.label.isNotBlank()) {
            Text(
                text = divider.label.uppercase(),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = lineColor,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
        DividerLine(
            color = lineColor,
            lineStyle = divider.lineStyle,
            strokeDp = strokeDp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Single horizontal line for a [DividerRow], drawn on a Canvas.
 * Supports SOLID, DASHED, and THICK styles.
 */
@Composable
private fun DividerLine(
    color: Color,
    lineStyle: DividerLineStyle,
    strokeDp: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokePx = with(density) { strokeDp.toPx() }

    Canvas(modifier = modifier.height((strokeDp + 8.dp).coerceAtLeast(8.dp))) {
        val y = size.height / 2f
        val pathEffect = if (lineStyle == DividerLineStyle.DASHED)
            PathEffect.dashPathEffect(floatArrayOf(12f, 7f), 0f)
        else
            null
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokePx,
            pathEffect = pathEffect
        )
    }
}

@Composable
private fun PersonCard(
    personWithBalance: PersonWithBalance,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        PersonAvatar(
            name = personWithBalance.person.name,
            avatarColor = personWithBalance.person.avatarColor,
            size = 44.dp,
            avatarImagePath = personWithBalance.person.avatarImagePath
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = personWithBalance.person.name,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BugListColors.Platinum
            )
            if (personWithBalance.openCount > 0) {
                Text(
                    text = "${personWithBalance.openCount} ${stringResource(R.string.dashboard_open_debts)}",
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 12.sp,
                    color = BugListColors.Muted
                )
            }
        }
        AmountText(
            amount = personWithBalance.netBalance,
            fontSize = 20.sp
        )
    }
}

// ── Summary header ────────────────────────────────────────────────────────────

@Composable
private fun DashboardSummaryHeader(
    totalBalance: Double,
    totalOwedToMe: Double,
    totalIOwe: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BugListColors.Surface)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard_total_balance),
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = BugListColors.Muted,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(4.dp))
        AmountText(
            amount = totalBalance,
            fontSize = 48.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BalanceTile(
                label = stringResource(R.string.dashboard_owed_to_me),
                amount = totalOwedToMe,
                positive = true,
                modifier = Modifier.weight(1f)
            )
            BalanceTile(
                label = stringResource(R.string.dashboard_i_owe),
                amount = totalIOwe,
                positive = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BalanceTile(
    label: String,
    amount: Double,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BugListColors.SurfaceHigh,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = BugListColors.Muted,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            AmountText(
                amount = if (positive) amount else -amount,
                fontSize = 22.sp
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun DashboardEmptyState(onAddItem: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_empty_title),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = BugListColors.Gold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.dashboard_empty_subtitle),
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 14.sp,
                color = BugListColors.Muted,
                textAlign = TextAlign.Center
            )
        }
    }
}
