package com.buglist.presentation.person_detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.buglist.R
import com.buglist.domain.model.PairingLink
import com.buglist.domain.model.SyncRole
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoupleSheet(
    personName: String,
    link: PairingLink?,
    code: String?,
    error: String?,
    busy: Boolean,
    synced: Boolean,
    onStart: () -> Unit,
    onAccept: (String) -> Unit,
    onClaim: (host: Boolean) -> Unit,
    onUnlink: () -> Unit,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentSync by rememberUpdatedState(onSyncNow)
    val step = couplingStep(link, synced, busy)
    // Resume an existing connection with a fresh check whenever the wizard opens.
    LaunchedEffect(Unit) { currentSync() }
    LaunchedEffect(link != null, step == CouplingStep.DONE, lifecycle) {
        if (link == null || step == CouplingStep.DONE) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(4_000)
                currentSync() // ViewModel prevents overlapping requests.
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        containerColor = BugListColors.Surface
    ) {
        CoupleWizardContent(personName, link, code, error, busy, synced,
            onStart, onAccept, onClaim, onUnlink, onSyncNow, onDismiss)
    }
}

@Composable
internal fun CoupleWizardContent(
    personName: String,
    link: PairingLink?,
    code: String?,
    error: String?,
    busy: Boolean,
    synced: Boolean,
    onStart: () -> Unit,
    onAccept: (String) -> Unit,
    onClaim: (Boolean) -> Unit,
    onUnlink: () -> Unit,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val step = couplingStep(link, synced, busy)
    var scanning by rememberSaveable { mutableStateOf(false) }
    var entering by rememberSaveable { mutableStateOf(false) }
    var pasted by rememberSaveable { mutableStateOf("") }
    var selectedHost by rememberSaveable { mutableStateOf(link?.wantedHost) }
    var confirmUnlink by remember { mutableStateOf(false) }
    LaunchedEffect(link?.wantedHost) { selectedHost = link?.wantedHost }
    if (confirmUnlink) {
        AlertDialog(onDismissRequest = { confirmUnlink = false },
            title = { Text(stringResource(R.string.wizard_unlink)) },
            text = { Text(stringResource(R.string.wizard_unlink_info)) },
            confirmButton = { TextButton(onClick = { confirmUnlink = false; onUnlink() }) {
                Text(stringResource(R.string.wizard_unlink))
            } },
            dismissButton = { TextButton(onClick = { confirmUnlink = false }) {
                Text(stringResource(R.string.action_cancel))
            } })
    }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.wizard_title), modifier = Modifier.weight(1f),
                fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold, fontSize = 23.sp,
                color = BugListColors.Gold)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, stringResource(R.string.settlement_close), tint = BugListColors.TextPrimary)
            }
        }
        WizardBody(stringResource(R.string.wizard_person, personName))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(when (step) {
            CouplingStep.CONNECT -> R.string.wizard_step_connect
            CouplingStep.ROLE -> R.string.wizard_step_role
            CouplingStep.VERIFY -> R.string.wizard_step_verify
            CouplingStep.DONE -> R.string.wizard_step_done
        }), fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold,
            color = BugListColors.TextPrimary, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { ((step.ordinal + 1).coerceAtMost(3)) / 3f },
            modifier = Modifier.fillMaxWidth(), color = BugListColors.Gold)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (step) {
                CouplingStep.CONNECT -> {
                    WizardBody(stringResource(R.string.wizard_connect_info))
                    if (code != null && !scanning && !entering) {
                        Image(remember(code) { qrBitmap(code).asImageBitmap() },
                            stringResource(R.string.wizard_qr), Modifier.fillMaxWidth().height(230.dp))
                        WizardBody(stringResource(R.string.wizard_scan_other))
                        SelectionContainer { WizardBody(code) }
                        WizardBody(stringResource(R.string.wizard_wait_device), BugListColors.Gold)
                    } else if (link != null && !scanning && !entering) {
                        GoldButton(stringResource(R.string.couple_show_code), onStart, enabled = !busy)
                        WizardBody(stringResource(R.string.wizard_wait_device))
                    } else if (link == null) {
                        GoldButton(stringResource(R.string.couple_show), onStart, enabled = !busy)
                    }
                    GoldButton(stringResource(if (scanning) R.string.wizard_stop_camera
                        else if (link != null) R.string.wizard_scan_instead else R.string.couple_scan),
                        { scanning = !scanning; entering = false }, enabled = !busy)
                    TextButton(onClick = { entering = !entering; scanning = false }, enabled = !busy) {
                        Text(stringResource(R.string.wizard_manual_code), color = BugListColors.Gold)
                    }
                    if (scanning) {
                        QrScanner { scanned -> scanning = false; onAccept(scanned) }
                        WizardBody(stringResource(R.string.wizard_camera_hint))
                    }
                    if (entering) {
                        OutlinedTextField(pasted, { pasted = it }, modifier = Modifier.fillMaxWidth(),
                            enabled = !busy, singleLine = true,
                            label = { Text(stringResource(R.string.couple_paste_hint)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                        GoldButton(stringResource(R.string.couple_link), { onAccept(pasted.trim()) },
                            enabled = pasted.isNotBlank() && !busy)
                    }
                }
                CouplingStep.ROLE -> {
                    WizardBody(stringResource(R.string.wizard_role_info))
                    RoleChoice(stringResource(R.string.wizard_host), stringResource(R.string.wizard_host_info),
                        selectedHost == true, !busy) { selectedHost = true }
                    RoleChoice(stringResource(R.string.wizard_guest), stringResource(R.string.wizard_guest_info),
                        selectedHost == false, !busy) { selectedHost = false }
                    if (selectedHost == false) {
                        WizardBody(stringResource(R.string.wizard_guest_replace), BugListColors.Orange)
                    }
                    if (link?.wantedHost != null) {
                        WizardBody(stringResource(R.string.wizard_your_confirmation,
                            stringResource(if (link.wantedHost) R.string.wizard_host else R.string.wizard_guest)))
                    }
                    WizardBody(if (link?.peerHost == null) stringResource(R.string.wizard_peer_wait)
                        else stringResource(R.string.wizard_peer_confirmation,
                            stringResource(if (link.peerHost) R.string.wizard_host else R.string.wizard_guest)))
                    if (link?.wantedHost != null && link.peerHost == link.wantedHost) {
                        WizardBody(stringResource(if (link.wantedHost) R.string.wizard_conflict_host
                            else R.string.wizard_conflict_guest), BugListColors.Orange)
                    }
                    GoldButton(stringResource(R.string.wizard_confirm_role), { selectedHost?.let(onClaim) },
                        enabled = selectedHost != null && !busy && selectedHost != link?.wantedHost)
                }
                CouplingStep.VERIFY -> {
                    WizardBody(stringResource(R.string.wizard_verifying))
                    WizardBody(stringResource(if (link?.role == SyncRole.HOST) R.string.wizard_host_info else R.string.wizard_guest_info))
                    GoldButton(stringResource(R.string.wizard_retry), onSyncNow, enabled = !busy)
                }
                CouplingStep.DONE -> {
                    WizardBody(stringResource(R.string.wizard_success), BugListColors.DebtGreen)
                    WizardBody(stringResource(if (link?.role == SyncRole.HOST) R.string.wizard_done_host else R.string.wizard_done_guest))
                    WizardBody(stringResource(R.string.wizard_usage))
                    GoldButton(stringResource(R.string.wizard_finish), onDismiss)
                    TextButton(onClick = onSyncNow, enabled = !busy) {
                        Text(stringResource(R.string.wizard_refresh), color = BugListColors.Gold)
                    }
                }
            }
            if (busy) {
                WizardBody(stringResource(R.string.wizard_busy), BugListColors.Gold)
            }
            if (!error.isNullOrBlank()) {
                WizardBody(error, BugListColors.Orange)
                if (step != CouplingStep.VERIFY) {
                    TextButton(onClick = onSyncNow, enabled = !busy) {
                        Text(stringResource(R.string.wizard_retry), color = BugListColors.Gold)
                    }
                }
            }
            if (link != null) {
                TextButton(onClick = { confirmUnlink = true }, enabled = !busy) {
                    Text(stringResource(R.string.wizard_unlink), color = BugListColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun WizardBody(text: String, color: Color = BugListColors.TextSecondary) {
    Text(text, color = color, fontFamily = RobotoCondensedFontFamily, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun RoleChoice(title: String, detail: String, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Surface(onClick = onSelect, enabled = enabled, shape = RoundedCornerShape(16.dp),
        color = BugListColors.SurfaceElevated,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BugListColors.Gold else BugListColors.Divider)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = null, enabled = enabled)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(title, color = BugListColors.TextPrimary, fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp)
                WizardBody(detail)
            }
        }
    }
}
