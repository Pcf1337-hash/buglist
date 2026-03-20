package com.buglist.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.data.remote.UpdateState
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import com.buglist.util.DownloadState

/**
 * Full-lifecycle update dialog for the startup update check.
 *
 * States handled:
 * - [DownloadState.Idle]         → Shows version info + UPDATE / SKIP / LATER buttons.
 * - [DownloadState.Downloading]  → Shows [LinearProgressIndicator] and disables buttons.
 * - [DownloadState.Ready]        → Shows "Bereit zum Installieren" + INSTALLIEREN button.
 * - [DownloadState.Error]        → Shows error message + RETRY / ABBRECHEN buttons.
 *
 * @param updateState    The update info — must be [UpdateState.UpdateAvailable].
 * @param downloadState  Current download lifecycle phase.
 * @param onUpdate       Called when the user taps UPDATE (starts download).
 * @param onInstall      Called when the user taps INSTALLIEREN (launches install intent).
 * @param onRetry        Called when the user taps RETRY after a failed download.
 * @param onSkip         Called when the user taps SKIP — version is persisted as skipped.
 * @param onDismiss      Called when the user taps LATER — dialog closes, no skip recorded.
 */
@Composable
fun StartupUpdateDialog(
    updateState: UpdateState.UpdateAvailable,
    downloadState: DownloadState,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            // Only allow dismiss by back-press when not downloading
            if (downloadState !is DownloadState.Downloading) onDismiss()
        },
        containerColor = BugListColors.Surface,
        titleContentColor = BugListColors.Gold,
        textContentColor = BugListColors.Platinum,
        title = {
            Text(
                text = when (downloadState) {
                    is DownloadState.Downloading -> "WIRD GELADEN…"
                    is DownloadState.Ready -> "BEREIT ZUM INSTALLIEREN"
                    is DownloadState.Error -> "DOWNLOAD FEHLGESCHLAGEN"
                    else -> "UPDATE VERFÜGBAR"
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Version badges — always shown
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UpdateVersionBadge(
                        text = "v${updateState.currentVersion}",
                        color = BugListColors.Muted
                    )
                    Text("→", color = BugListColors.Gold, fontWeight = FontWeight.Bold)
                    UpdateVersionBadge(
                        text = "v${updateState.newVersion}",
                        color = BugListColors.DebtGreen
                    )
                }

                // Phase-specific content
                when (downloadState) {
                    is DownloadState.Idle -> {
                        if (!updateState.releaseNotes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Scrollable changelog card — max 200dp, scrolls inside
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .border(
                                        width = 1.dp,
                                        color = BugListColors.Divider,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                val scrollState = rememberScrollState()
                                Text(
                                    text = updateState.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = RobotoCondensedFontFamily,
                                        lineHeight = androidx.compose.ui.unit.TextUnit(
                                            18f,
                                            androidx.compose.ui.unit.TextUnitType.Sp
                                        )
                                    ),
                                    color = BugListColors.Platinum,
                                    modifier = Modifier
                                        .verticalScroll(scrollState)
                                        .padding(12.dp)
                                )
                            }
                        }
                    }

                    is DownloadState.Downloading -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        val progressText = if (downloadState.progressFraction > 0f) {
                            "${(downloadState.progressFraction * 100).toInt()} %"
                        } else {
                            "Verbinde…"
                        }
                        Text(
                            text = progressText,
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 12.sp,
                            color = BugListColors.Muted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (downloadState.progressFraction > 0f) {
                            LinearProgressIndicator(
                                progress = { downloadState.progressFraction },
                                modifier = Modifier.fillMaxWidth(),
                                color = BugListColors.Gold,
                                trackColor = BugListColors.Divider
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = BugListColors.Gold,
                                trackColor = BugListColors.Divider
                            )
                        }
                    }

                    is DownloadState.Ready -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Update heruntergeladen. Tippe INSTALLIEREN um die neue Version einzuspielen.",
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 13.sp,
                            color = BugListColors.Muted
                        )
                    }

                    is DownloadState.Error -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = downloadState.message,
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 13.sp,
                            color = BugListColors.DebtRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is DownloadState.Idle -> {
                    GoldButton(
                        text = "UPDATE",
                        onClick = onUpdate,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                is DownloadState.Downloading -> {
                    GoldButton(
                        text = "LÄDT…",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                is DownloadState.Ready -> {
                    GoldButton(
                        text = "INSTALLIEREN",
                        onClick = onInstall,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                is DownloadState.Error -> {
                    GoldButton(
                        text = "ERNEUT VERSUCHEN",
                        onClick = onRetry,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        },
        dismissButton = {
            if (downloadState !is DownloadState.Downloading) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (downloadState is DownloadState.Idle) {
                        TextButton(onClick = onSkip) {
                            Text(
                                text = "ÜBERSPRINGEN",
                                color = BugListColors.Muted,
                                fontFamily = RobotoCondensedFontFamily
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "SPÄTER",
                            color = BugListColors.Platinum,
                            fontFamily = RobotoCondensedFontFamily
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun UpdateVersionBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        modifier = Modifier.border(
            width = 1.dp,
            color = color,
            shape = RoundedCornerShape(4.dp)
        )
    ) {
        Text(
            text = text,
            fontFamily = RobotoCondensedFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = color,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
