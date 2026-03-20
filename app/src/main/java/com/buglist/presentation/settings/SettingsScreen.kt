package com.buglist.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.BuildConfig
import com.buglist.R
import com.buglist.data.remote.UpdateState
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.components.UpdateDialog
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import java.io.File

/**
 * Settings screen — currency, auto-lock timeout, export, reset.
 *
 * @param onBack       Navigate up (back button in top bar).
 * @param onDeleteAll  Called after all data has been successfully deleted so the caller
 *                     can navigate back to the dashboard / auth screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDeleteAll: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiData by viewModel.uiData.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate away once all data has been deleted.
    LaunchedEffect(Unit) {
        viewModel.deleteAllEvent.collect {
            onDeleteAll()
        }
    }

    // Show snackbar feedback for update check results that don't have their own dialog.
    // Without this the user sees no reaction at all after pressing "AUF UPDATES PRÜFEN".
    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateState.UpToDate -> {
                snackbarHostState.showSnackbar("Du hast die neueste Version \u2713")
                viewModel.onUpdateDismissed()
            }
            is UpdateState.NoConnection -> {
                snackbarHostState.showSnackbar("Keine Verbindung \u2013 check dein Internet")
                viewModel.onUpdateDismissed()
            }
            is UpdateState.Error -> {
                snackbarHostState.showSnackbar("Update-Prüfung fehlgeschlagen: ${state.message}")
                viewModel.onUpdateDismissed()
            }
            else -> Unit
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

    // Share CSV when export completes
    LaunchedEffect(uiData.exportCsv) {
        val csv = uiData.exportCsv ?: return@LaunchedEffect
        try {
            val file = File(context.cacheDir, "buglist_export.csv")
            file.writeText(csv)
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BugList Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export CSV"))
        } finally {
            viewModel.clearExportCsv()
        }
    }

    if (uiData.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = {
                Text(
                    stringResource(R.string.settings_delete_all_title),
                    fontFamily = OswaldFontFamily,
                    color = BugListColors.DebtRed
                )
            },
            text = {
                Text(
                    stringResource(R.string.settings_delete_all_message),
                    fontFamily = RobotoCondensedFontFamily,
                    color = BugListColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllData()
                }) {
                    Text(stringResource(R.string.action_delete), color = BugListColors.DebtRed)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) {
                    Text(stringResource(R.string.action_cancel), color = BugListColors.Muted)
                }
            },
            containerColor = BugListColors.Surface
        )
    }

    Scaffold(
        containerColor = BugListColors.Background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings).uppercase(),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BugListColors.Gold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = BugListColors.Platinum
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BugListColors.Background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_preferences)) {
                    CurrencyDropdown(
                        selected = uiData.currency,
                        onSelect = viewModel::setCurrency
                    )
                    Spacer(Modifier.height(12.dp))
                    AutoLockDropdown(
                        selectedSeconds = uiData.autoLockTimeoutSeconds,
                        onSelect = viewModel::setAutoLockTimeout
                    )
                }
            }
            item {
                TagsSection(
                    allTags = allTags,
                    onAddTag = viewModel::addTag,
                    onDeleteTag = viewModel::deleteTag
                )
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_section_data)) {
                    GoldButton(
                        text = stringResource(R.string.settings_export_button),
                        onClick = viewModel::exportData,
                        enabled = !uiData.isExporting
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = viewModel::showDeleteConfirm,
                        shape = RoundedCornerShape(8.dp),
                        color = BugListColors.DebtRed.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_delete_all_button).uppercase(),
                            fontFamily = OswaldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BugListColors.DebtRed,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            if (BuildConfig.DEBUG) {
                item {
                    SettingsSection(title = stringResource(R.string.settings_section_debug)) {
                        Surface(
                            onClick = { viewModel.seedTestData() },
                            shape = RoundedCornerShape(8.dp),
                            color = BugListColors.Gold.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (uiData.isSeedingData)
                                    "GENERIERE DATEN..."
                                else
                                    stringResource(R.string.settings_seed_data_button),
                                fontFamily = OswaldFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (uiData.isSeedingData) BugListColors.GoldDim else BugListColors.Gold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_version_label),
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 14.sp,
                            color = BugListColors.Muted,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            fontFamily = RobotoCondensedFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = BugListColors.Platinum
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    GoldButton(
                        text = "AUF UPDATES PRÜFEN",
                        onClick = viewModel::checkForUpdateManually,
                        enabled = updateState !is UpdateState.Checking
                    )
                }
            }
        }
    }
}

/**
 * Settings section for managing user-defined tags.
 *
 * Displays existing tags as deletable [InputChip]s and provides an input field
 * + button to add new tags. Enforces max. 20 characters per tag and max. 30 tags total.
 *
 * @param allTags     Current list of tags from the repository.
 * @param onAddTag    Called with the new tag name when the user confirms.
 * @param onDeleteTag Called with the tag to delete when the X is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    allTags: List<com.buglist.domain.model.Tag>,
    onAddTag: (String) -> Unit,
    onDeleteTag: (com.buglist.domain.model.Tag) -> Unit
) {
    var newTagText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.settings_tags_section),
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = BugListColors.Muted,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BugListColors.Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Chip grid of existing tags
                if (allTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_tags_empty),
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 13.sp,
                        color = BugListColors.Muted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = {
                                    Text(
                                        text = tag.name,
                                        fontFamily = RobotoCondensedFontFamily,
                                        fontSize = 13.sp
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onDeleteTag(tag) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(
                                                R.string.tag_delete_content_description,
                                                tag.name
                                            ),
                                            modifier = Modifier.size(14.dp),
                                            tint = BugListColors.Muted
                                        )
                                    }
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = BugListColors.SurfaceHigh,
                                    labelColor = BugListColors.Platinum
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = BugListColors.Divider
                                )
                            )
                        }
                    }
                }

                // New tag input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { if (it.length <= 20) newTagText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.settings_tags_add_hint),
                                fontFamily = RobotoCondensedFontFamily,
                                color = BugListColors.Muted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BugListColors.Gold,
                            unfocusedBorderColor = BugListColors.Divider,
                            focusedTextColor = BugListColors.Platinum,
                            unfocusedTextColor = BugListColors.Platinum,
                            cursorColor = BugListColors.Gold
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newTagText.isNotBlank()) {
                                onAddTag(newTagText)
                                newTagText = ""
                            }
                        })
                    )
                    Button(
                        onClick = {
                            if (newTagText.isNotBlank()) {
                                onAddTag(newTagText)
                                newTagText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BugListColors.Gold,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "+",
                            fontFamily = OswaldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = BugListColors.Muted,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BugListColors.Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(selected: String, onSelect: (String) -> Unit) {
    val currencies = listOf("EUR", "USD", "CHF", "GBP", "JPY")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(stringResource(R.string.settings_currency_label),
                    fontFamily = RobotoCondensedFontFamily, color = BugListColors.Muted)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BugListColors.Gold,
                unfocusedBorderColor = BugListColors.Divider,
                focusedTextColor = BugListColors.Platinum,
                unfocusedTextColor = BugListColors.Platinum
            ),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BugListColors.SurfaceHigh
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Text(currency,
                            fontFamily = RobotoCondensedFontFamily,
                            color = BugListColors.Platinum)
                    },
                    onClick = { onSelect(currency); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoLockDropdown(selectedSeconds: Int, onSelect: (Int) -> Unit) {
    val options = listOf(
        30 to "30 Sekunden",
        60 to "1 Minute",
        120 to "2 Minuten",
        300 to "5 Minuten",
        0 to "Sofort"
    )
    var expanded by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selectedSeconds }?.second ?: "$selectedSeconds s"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(stringResource(R.string.settings_auto_lock_label),
                    fontFamily = RobotoCondensedFontFamily, color = BugListColors.Muted)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BugListColors.Gold,
                unfocusedBorderColor = BugListColors.Divider,
                focusedTextColor = BugListColors.Platinum,
                unfocusedTextColor = BugListColors.Platinum
            ),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BugListColors.SurfaceHigh
        ) {
            options.forEach { (seconds, text) ->
                DropdownMenuItem(
                    text = {
                        Text(text,
                            fontFamily = RobotoCondensedFontFamily,
                            color = BugListColors.Platinum)
                    },
                    onClick = { onSelect(seconds); expanded = false }
                )
            }
        }
    }
}
