package com.buglist.presentation.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.data.remote.UpdateState
import com.buglist.domain.model.DashboardListItem
import com.buglist.domain.model.Divider
import com.buglist.domain.model.DividerLineStyle
import com.buglist.domain.model.PersonWithBalance
import com.buglist.presentation.add_divider.AddDividerSheet
import com.buglist.presentation.components.AmountText
import com.buglist.presentation.components.StreetEmptyState
import com.buglist.presentation.components.formatAmount
import com.buglist.presentation.components.PersonAvatar
import com.buglist.presentation.components.UpdateDialog
import com.buglist.presentation.theme.BebasNeueFontFamily
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import com.buglist.util.KEY_AMBIENT_MOTION
import com.buglist.util.appDataStore
import kotlinx.coroutines.flow.map
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Dashboard screen — shows all persons with net balances and the global total.
 *
 * The crew list renders both [DashboardListItem.PersonItem] rows and
 * [DashboardListItem.DividerItem] separator rows interleaved by sort index.
 *
 * Standard [HorizontalDivider] separators are suppressed when a custom divider
 * row is adjacent, so the two types of separators never overlap visually.
 *
 * Supports manual drag-to-reorder. In edit mode, each divider row shows a
 * delete button (trash) and an edit button (pencil) in addition to the drag handle.
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
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context    = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    // Edit-mode mutable list copy
    var editMode      by remember { mutableStateOf(false) }
    var editableItems by remember { mutableStateOf<List<DashboardListItem>>(emptyList()) }

    // Divider being edited — non-null opens the AddDividerSheet in edit mode
    var dividerToEdit by remember { mutableStateOf<Divider?>(null) }

    // Reorderable state
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
            onSkip   = { viewModel.onUpdateSkipped(update.newVersion) },
            onDismiss = { viewModel.onUpdateDismissed() }
        )
    }

    // Edit divider sheet (shown on top of dashboard)
    if (dividerToEdit != null) {
        AddDividerSheet(
            existingDivider = dividerToEdit,
            onDismiss = { dividerToEdit = null },
            onSaved   = { _ -> dividerToEdit = null }
        )
    }

    // Feature B: search visibility toggle. The title stays put; the field opens under the bar
    // so the wordmark the user expects does not disappear, and the field is a full thumb target.
    var searchActive by remember { mutableStateOf(false) }
    var inactiveExpanded by rememberSaveable { mutableStateOf(false) }
    val groupInactive by viewModel.groupInactive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            // The field must be in the tree before focus, or key events (and adb text) miss it.
            kotlinx.coroutines.delay(80)
            runCatching { searchFocus.requestFocus() }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
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
                            if (searchActive) {
                                IconButton(onClick = {
                                    searchActive = false
                                    viewModel.onSearchQueryChanged("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.dashboard_search_close),
                                        tint = BugListColors.Gold
                                    )
                                }
                            } else {
                                IconButton(onClick = { searchActive = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.dashboard_search_hint),
                                        tint = BugListColors.TextPrimary
                                    )
                                }
                                IconButton(onClick = onStatistics) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = stringResource(R.string.nav_statistics),
                                        tint = BugListColors.TextPrimary
                                    )
                                }
                                IconButton(onClick = onSettings) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.nav_settings),
                                        tint = BugListColors.TextPrimary
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BugListColors.SurfaceDark)
                )
                if (searchActive && !editMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 18.sp,
                            color = BugListColors.TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        label = {
                            Text(
                                text = stringResource(R.string.dashboard_search_hint),
                                fontFamily = RobotoCondensedFontFamily,
                                fontSize = 16.sp,
                                color = BugListColors.TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BugListColors.TextPrimary,
                            unfocusedTextColor = BugListColors.TextPrimary,
                            cursorColor = BugListColors.Gold,
                            focusedBorderColor = BugListColors.Gold,
                            unfocusedBorderColor = BugListColors.BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .focusRequester(searchFocus)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                HorizontalDivider(color = BugListColors.BorderSubtle, thickness = 0.5.dp)
            }
        },
        floatingActionButton = {
            if (!editMode) {
                FloatingActionButton(
                    onClick = onAddItem,
                    modifier = Modifier.size(64.dp),
                    containerColor = BugListColors.Gold,
                    contentColor = BugListColors.SurfaceDark
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.dashboard_add_person),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { paddingValues ->

        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    CircularProgressIndicator(color = BugListColors.Gold)
                }
            }

            is DashboardUiState.Ready -> {
                if (state.persons.isEmpty() && state.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        DashboardEmptyState(onAddItem = onAddItem)
                    }
                } else if (editMode) {
                    // ── EDIT MODE ────────────────────────────────────────────
                    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        DashboardSummaryHeader(
                            totalBalance  = state.totalBalance,
                            totalOwedToMe = state.totalOwedToMe,
                            totalIOwe     = state.totalIOwe
                        )
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
                                fontSize = 13.sp,
                                color = BugListColors.Gold,
                                letterSpacing = 3.sp,
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
                        LazyColumn(
                            state = reorderLazyListState,
                            contentPadding = PaddingValues(bottom = 112.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(editableItems, key = { _, item -> item.listKey }) { _, item ->
                                ReorderableItem(reorderableState, key = item.listKey) { isDragging ->
                                    val elevation by animateDpAsState(
                                        targetValue = if (isDragging) 8.dp else 0.dp,
                                        label = "drag_elevation"
                                    )
                                    Surface(
                                        shadowElevation = elevation,
                                        color = if (isDragging) BugListColors.SurfaceOverlay
                                                else BugListColors.SurfaceDark
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
                                                    onEdit   = { dividerToEdit = item.data },
                                                    onDelete = {
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
                    // ── NORMAL MODE ──────────────────────────────────────────
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh    = { isRefreshing = false },
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 112.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Hide summary header when search is active
                            if (!searchActive) {
                                item {
                                    DashboardSummaryHeader(
                                        totalBalance  = state.totalBalance,
                                        totalOwedToMe = state.totalOwedToMe,
                                        totalIOwe     = state.totalIOwe,
                                        showCharm     = true
                                    )
                                }
                            }
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.dashboard_crew_header),
                                        fontFamily = OswaldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BugListColors.TextSecondary,
                                        letterSpacing = 3.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!searchActive) {
                                        IconButton(
                                            onClick = {
                                                editableItems = state.items
                                                editMode = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Reihenfolge anpassen",
                                                tint = BugListColors.TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Feature B: empty search state
                            if (searchActive && state.filteredItems.isEmpty()) {
                                item {
                                    StreetEmptyState(
                                        title = stringResource(R.string.dashboard_search_empty)
                                    )
                                }
                            }

                            val foldInactive = groupInactive && !searchActive
                            val inactivePeople = if (foldInactive) {
                                state.filteredItems.filter { item ->
                                    item is DashboardListItem.PersonItem && item.data.openCount == 0
                                }
                            } else {
                                emptyList()
                            }
                            val displayItems = if (foldInactive) {
                                state.filteredItems.filter { item ->
                                    item !is DashboardListItem.PersonItem || item.data.openCount > 0
                                }
                            } else {
                                state.filteredItems
                            }
                            itemsIndexed(
                                items = displayItems,
                                key   = { _, item -> item.listKey }
                            ) { index, item ->
                                when (item) {
                                    is DashboardListItem.PersonItem -> {
                                        PersonCard(
                                            personWithBalance = item.data,
                                            onClick = { onPersonClick(item.data.person.id) }
                                        )
                                        // Suppress thin separator if next item is a custom DividerItem
                                        val nextItem = displayItems.getOrNull(index + 1)
                                        if (nextItem !is DashboardListItem.DividerItem) {
                                            HorizontalDivider(
                                                color = BugListColors.BorderSubtle,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                    is DashboardListItem.DividerItem -> {
                                        DividerRow(divider = item.data)
                                    }
                                }
                            }
                            if (inactivePeople.isNotEmpty()) {
                                item(key = "inactive-header") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 56.dp)
                                            .clickable { inactiveExpanded = !inactiveExpanded }
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.dashboard_inactive_header,
                                                inactivePeople.size
                                            ),
                                            fontFamily = OswaldFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = BugListColors.TextSecondary,
                                            letterSpacing = 2.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (inactiveExpanded) {
                                                Icons.Default.ExpandLess
                                            } else {
                                                Icons.Default.ExpandMore
                                            },
                                            contentDescription = stringResource(
                                                R.string.settings_group_inactive
                                            ),
                                            tint = BugListColors.TextMuted
                                        )
                                    }
                                }
                                if (inactiveExpanded) {
                                    items(
                                        items = inactivePeople,
                                        key = { item -> item.listKey }
                                    ) { item ->
                                        val person = item as DashboardListItem.PersonItem
                                        PersonCard(
                                            personWithBalance = person.data,
                                            onClick = { onPersonClick(person.data.person.id) }
                                        )
                                        HorizontalDivider(
                                            color = BugListColors.BorderSubtle,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
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

// ── Editable row composables ──────────────────────────────────────────────────

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
            name            = personWithBalance.person.name,
            avatarColor     = personWithBalance.person.avatarColor,
            size            = 44.dp,
            avatarImagePath = personWithBalance.person.avatarImagePath
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = personWithBalance.person.name,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BugListColors.TextPrimary
            )
            if (personWithBalance.openCount > 0) {
                Text(
                    text = "${personWithBalance.openCount} ${stringResource(R.string.dashboard_open_debts)}",
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 12.sp,
                    color = BugListColors.TextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Ziehen zum Sortieren",
            tint = BugListColors.TextMuted,
            modifier = dragHandleModifier.size(24.dp)
        )
    }
}

@Composable
private fun EditableDividerRow(
    divider: Divider,
    dragHandleModifier: Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
    ) {
        // Edit button
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.divider_edit_content_description),
                tint = BugListColors.Gold,
                modifier = Modifier.size(18.dp)
            )
        }
        // Delete button
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.divider_delete_content_description),
                tint = BugListColors.DebtRed,
                modifier = Modifier.size(18.dp)
            )
        }
        // Divider preview
        Box(modifier = Modifier.weight(1f)) {
            DividerRow(divider = divider)
        }
        // Drag handle
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Ziehen zum Sortieren",
            tint = BugListColors.Muted,
            modifier = dragHandleModifier.size(24.dp)
        )
    }
}

// ── DividerRow — all 7 styles ─────────────────────────────────────────────────

/**
 * Renders a [Divider] separator row as it appears in the crew list and in the
 * AddDividerSheet live preview.
 *
 * Supported styles:
 * - SOLID           → `──── LABEL ────`
 * - DASHED          → `- - - LABEL - - -`
 * - THICK           → `━━━━ LABEL ━━━━`
 * - DIAMOND_STAR    → `──── ✦ LABEL ✦ ────`
 * - BRACKET         → `──── [ LABEL ] ────`
 * - ARROW           → `<<< LABEL >>>`
 * - DIAMOND_FLANKED → `❖──── LABEL ────❖`
 *
 * Public so [com.buglist.presentation.add_divider.AddDividerSheet] can render the live preview.
 */
@Composable
fun DividerRow(divider: Divider) {
    val lineColor = Color(divider.color)

    if (divider.lineStyle == DividerLineStyle.ARROW) {
        ArrowDividerRow(label = divider.label, color = lineColor)
    } else {
        LineDividerRow(divider = divider, lineColor = lineColor)
    }
}

/** `<<< LABEL >>>` — no lines, purely text-based. */
@Composable
private fun ArrowDividerRow(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "<<<",
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color,
            letterSpacing = 2.sp
        )
        if (label.isNotBlank()) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = label.uppercase(),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = ">>>",
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color,
            letterSpacing = 2.sp
        )
    }
}

/**
 * Line-based divider row for all non-ARROW styles.
 *
 * Layout:
 * - DIAMOND_FLANKED: `❖` | [line] | [label] | [line] | `❖`
 * - DIAMOND_STAR:    [line] | `✦` | [label] | `✦` | [line]
 * - BRACKET:         [line] | `[` | [label] | `]` | [line]
 * - SOLID/DASHED/THICK: [line] | [label] | [line]
 */
@Composable
private fun LineDividerRow(divider: Divider, lineColor: Color) {
    val strokeDp: Dp = if (divider.lineStyle == DividerLineStyle.THICK) 3.dp else 1.dp
    val dashed = divider.lineStyle == DividerLineStyle.DASHED

    // Outer flanking characters (outside the lines)
    val outerLeft  = if (divider.lineStyle == DividerLineStyle.DIAMOND_FLANKED) "❖" else ""
    val outerRight = if (divider.lineStyle == DividerLineStyle.DIAMOND_FLANKED) "❖" else ""

    // Inner decorators (between line and label)
    val (innerLeft, innerRight) = when (divider.lineStyle) {
        DividerLineStyle.DIAMOND_STAR -> "✦" to "✦"
        DividerLineStyle.BRACKET      -> "[" to "]"
        else                          -> "" to ""
    }

    val hasCenter = divider.label.isNotBlank() || innerLeft.isNotEmpty()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (outerLeft.isNotEmpty()) {
            Text(
                text = outerLeft,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = lineColor
            )
        }

        DividerLine(
            color    = lineColor,
            dashed   = dashed,
            strokeDp = strokeDp,
            modifier = Modifier.weight(1f)
        )

        if (hasCenter) {
            Spacer(Modifier.width(6.dp))
            if (innerLeft.isNotEmpty()) {
                Text(
                    text = innerLeft,
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = lineColor
                )
                Spacer(Modifier.width(4.dp))
            }
            if (divider.label.isNotBlank()) {
                Text(
                    text = divider.label.uppercase(),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = lineColor,
                    letterSpacing = 2.sp
                )
            }
            if (innerRight.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = innerRight,
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = lineColor
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        DividerLine(
            color    = lineColor,
            dashed   = dashed,
            strokeDp = strokeDp,
            modifier = Modifier.weight(1f)
        )

        if (outerRight.isNotEmpty()) {
            Text(
                text = outerRight,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = lineColor
            )
        }
    }
}

/** Single horizontal line drawn on a Canvas — supports solid and dashed styles. */
@Composable
private fun DividerLine(
    color: Color,
    dashed: Boolean,
    strokeDp: Dp,
    modifier: Modifier = Modifier
) {
    val density  = LocalDensity.current
    val strokePx = with(density) { strokeDp.toPx() }

    Canvas(modifier = modifier.height((strokeDp + 8.dp).coerceAtLeast(8.dp))) {
        val y = size.height / 2f
        drawLine(
            color       = color,
            start       = Offset(0f, y),
            end         = Offset(size.width, y),
            strokeWidth = strokePx,
            pathEffect  = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 7f), 0f) else null
        )
    }
}

// ── Person card ───────────────────────────────────────────────────────────────

@Composable
private fun PersonCard(
    personWithBalance: PersonWithBalance,
    onClick: () -> Unit
) {
    val balance = personWithBalance.netBalance
    val accent = when {
        balance > 0.001 -> BugListColors.DebtGreen
        balance < -0.001 -> BugListColors.DebtRed
        else -> BugListColors.BorderSubtle
    }
    val openLabel = stringResource(R.string.dashboard_open_debts)
    val directionLabel = when {
        balance > 0.001 -> stringResource(R.string.dashboard_owed_to_me)
        balance < -0.001 -> stringResource(R.string.dashboard_i_owe)
        else -> stringResource(R.string.dashboard_total_balance)
    }
    val rowDescription = buildString {
        append(personWithBalance.person.name)
        if (personWithBalance.openCount > 0) {
            append(", ")
            append(personWithBalance.openCount)
            append(" ")
            append(openLabel)
        }
        append(", ")
        append(directionLabel)
        append(", ")
        append(formatAmount(kotlin.math.abs(balance), "EUR"))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = rowDescription }
            .background(BugListColors.SurfaceCard)
            .padding(end = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 10.dp, end = 12.dp)
                .width(4.dp)
                .height(48.dp)
                .background(accent, RoundedCornerShape(2.dp))
        )
        PersonAvatar(
            name            = personWithBalance.person.name,
            avatarColor     = personWithBalance.person.avatarColor,
            size            = 48.dp,
            avatarImagePath = personWithBalance.person.avatarImagePath
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp)
        ) {
            Text(
                text = personWithBalance.person.name,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = BugListColors.TextPrimary
            )
            if (personWithBalance.openCount > 0) {
                Text(
                    text = "${personWithBalance.openCount} ${stringResource(R.string.dashboard_open_debts)}",
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 13.sp,
                    color = BugListColors.TextSecondary
                )
            }
        }
        AmountText(
            amount = balance,
            fontSize = 28.sp
        )
    }
}

// ── Summary header ────────────────────────────────────────────────────────────

/**
 * Dashboard summary header with animated hero balance.
 *
 * The primary balance counts up from 0 to [totalBalance] on first load and whenever
 * the value changes (e.g. after a settlement). Animation: 1000ms FastOutSlowInEasing.
 * Tabular figures (tnum) keep digits fixed-width so the counter never shifts sideways.
 */
@Composable
private fun DashboardSummaryHeader(
    totalBalance: Double,
    totalOwedToMe: Double,
    totalIOwe: Double,
    showCharm: Boolean = false
) {
    // Color mirrors AmountText logic
    val heroColor = when {
        totalBalance > 0.001  -> BugListColors.DebtGreen
        totalBalance < -0.001 -> BugListColors.DebtRed
        else                  -> BugListColors.Gold
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BugListColors.SurfaceCard)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        val context = LocalContext.current
        val charmEnabled by remember(context) {
            context.appDataStore.data.map { it[KEY_AMBIENT_MOTION] ?: true }
        }.collectAsStateWithLifecycle(initialValue = false)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val charmWidth = (maxWidth * 0.50f).coerceAtMost(176.dp)
            val placeCharm = showCharm && charmEnabled && charmWidth >= 100.dp
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dashboard_total_balance),
                    fontFamily = RobotoCondensedFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = BugListColors.TextMuted,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(end = if (placeCharm) charmWidth else 0.dp)
                )
                Spacer(Modifier.height(if (placeCharm) charmWidth * 0.24f else 4.dp))
                AmountText(
                    amount = totalBalance,
                    fontSize = 72.sp,
                    modifier = Modifier.fillMaxWidth(),
                    forceColor = heroColor
                )
            }
            if (placeCharm) {
                GoldCharm(charmWidth, Modifier.align(Alignment.TopEnd))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BalanceTile(
                label    = stringResource(R.string.dashboard_owed_to_me),
                amount   = totalOwedToMe,
                positive = true,
                modifier = Modifier.weight(1f)
            )
            BalanceTile(
                label    = stringResource(R.string.dashboard_i_owe),
                amount   = totalIOwe,
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
    val accentColor = if (positive) BugListColors.DebtGreen else BugListColors.DebtRed
    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = BugListColors.SurfaceElevated,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$label, ${formatAmount(amount, "EUR")}"
                }
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .background(accentColor, RoundedCornerShape(1.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontFamily = RobotoCondensedFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = BugListColors.TextSecondary,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(2.dp))
            AmountText(
                amount   = if (positive) amount else -amount,
                fontSize = 28.sp
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
            // Large gold group icon
            StreetEmptyState(
                title = stringResource(R.string.dashboard_empty_title),
                subtitle = stringResource(R.string.dashboard_empty_subtitle),
                icon = Icons.Default.Add
            )
        }
    }
}
