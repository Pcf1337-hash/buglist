package com.buglist.presentation.dashboard

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.data.remote.UpdateState
import com.buglist.domain.model.PersonWithBalance
import com.buglist.presentation.components.AmountText
import com.buglist.presentation.components.PersonAvatar
import com.buglist.presentation.components.UpdateDialog
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily

/**
 * Dashboard screen — shows all persons with their net balances and the global total.
 *
 * @param onPersonClick   Navigate to person detail screen.
 * @param onAddPerson     Open add-person bottom sheet.
 * @param onStatistics    Navigate to statistics screen.
 * @param onSettings      Navigate to settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onPersonClick: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BugListColors.Background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPerson,
                containerColor = BugListColors.Gold,
                contentColor = BugListColors.Background
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.dashboard_add_person)
                )
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = false },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(color = BugListColors.Gold)
                    }
                }
                is DashboardUiState.Ready -> {
                    if (state.persons.isEmpty()) {
                        DashboardEmptyState(onAddPerson = onAddPerson)
                    } else {
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
                                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
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
                                    Box {
                                        IconButton(onClick = { showSortMenu = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Sort,
                                                contentDescription = "Sortierung",
                                                tint = if (sortOrder == com.buglist.domain.usecase.PersonSortOrder.NAME)
                                                    BugListColors.Muted
                                                else
                                                    BugListColors.Gold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false },
                                            containerColor = BugListColors.SurfaceHigh
                                        ) {
                                            SortOption(
                                                label = "Name A–Z",
                                                selected = sortOrder == com.buglist.domain.usecase.PersonSortOrder.NAME,
                                                onClick = {
                                                    viewModel.setSortOrder(com.buglist.domain.usecase.PersonSortOrder.NAME)
                                                    showSortMenu = false
                                                }
                                            )
                                            SortOption(
                                                label = "Betrag ↓",
                                                selected = sortOrder == com.buglist.domain.usecase.PersonSortOrder.BALANCE_DESC,
                                                onClick = {
                                                    viewModel.setSortOrder(com.buglist.domain.usecase.PersonSortOrder.BALANCE_DESC)
                                                    showSortMenu = false
                                                }
                                            )
                                            SortOption(
                                                label = "Zuletzt hinzugefügt",
                                                selected = sortOrder == com.buglist.domain.usecase.PersonSortOrder.CREATED_AT_DESC,
                                                onClick = {
                                                    viewModel.setSortOrder(com.buglist.domain.usecase.PersonSortOrder.CREATED_AT_DESC)
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            items(state.persons, key = { it.person.id }) { personWithBalance ->
                                PersonCard(
                                    personWithBalance = personWithBalance,
                                    onClick = { onPersonClick(personWithBalance.person.id) }
                                )
                                HorizontalDivider(
                                    color = BugListColors.Divider,
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
            size = 44.dp
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

@Composable
private fun SortOption(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontFamily = OswaldFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (selected) BugListColors.Gold else BugListColors.Platinum
            )
        },
        onClick = onClick
    )
}

@Composable
private fun DashboardEmptyState(onAddPerson: () -> Unit) {
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
