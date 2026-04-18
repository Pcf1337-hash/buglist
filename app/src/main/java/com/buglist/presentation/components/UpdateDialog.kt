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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.data.remote.UpdateState
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily

@Composable
fun UpdateDialog(
    updateState: UpdateState.UpdateAvailable,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BugListColors.Surface,
        titleContentColor = BugListColors.Gold,
        textContentColor = BugListColors.Platinum,
        title = {
            Text(
                text = "UPDATE AVAILABLE",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VersionBadge(text = "v${updateState.currentVersion}", color = BugListColors.Muted)
                    Text("→", color = BugListColors.Gold, fontWeight = FontWeight.Bold)
                    VersionBadge(text = "v${updateState.newVersion}", color = BugListColors.DebtGreen)
                }
                if (!updateState.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .border(
                                width = 1.dp,
                                color = BugListColors.Divider,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(
                            text = updateState.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = BugListColors.Muted,
                            fontFamily = RobotoCondensedFontFamily,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            GoldButton(
                text = "UPDATE",
                onClick = onUpdate
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "SKIP",
                        color = BugListColors.Muted,
                        fontFamily = RobotoCondensedFontFamily
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "LATER",
                        color = BugListColors.Platinum,
                        fontFamily = RobotoCondensedFontFamily
                    )
                }
            }
        }
    )
}

@Composable
private fun VersionBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        modifier = Modifier.border(width = 1.dp, color = color, shape = RoundedCornerShape(4.dp))
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
