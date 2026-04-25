package com.buglist.presentation.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily

/**
 * Password input dialog for backup export (two fields) and import (one field).
 *
 * @param isExport         true = export variant (password + confirm), false = import (single field).
 * @param isLoading        true while Argon2 KDF is running — shows spinner and disables confirm.
 * @param errorMessage     Non-null → shown in DebtRed below the field(s).
 * @param onConfirm        Called with the entered password when user taps "Bestätigen".
 * @param onDismiss        Called when user taps "Abbrechen".
 */
@Composable
fun BackupPasswordDialog(
    isExport: Boolean,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val mismatch = isExport && confirm.isNotEmpty() && password != confirm
    val tooShort = password.isNotEmpty() && password.length < 4
    val isWeak = password.length in 1..7
    val canConfirm = !isLoading && password.length >= 4 &&
            (!isExport || (confirm.isNotEmpty() && password == confirm))

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = BugListColors.Surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = stringResource(
                    if (isExport) R.string.backup_password_title_export
                    else R.string.backup_password_title_import
                ),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BugListColors.Gold,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            stringResource(R.string.backup_password_hint),
                            fontFamily = RobotoCondensedFontFamily,
                            color = BugListColors.Muted
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = BugListColors.Muted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BugListColors.Gold,
                        unfocusedBorderColor = BugListColors.Divider,
                        focusedTextColor = BugListColors.Platinum,
                        unfocusedTextColor = BugListColors.Platinum,
                        cursorColor = BugListColors.Gold
                    ),
                    isError = tooShort,
                    modifier = Modifier.fillMaxWidth()
                )

                // Confirm field (export only)
                if (isExport) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = {
                            Text(
                                stringResource(R.string.backup_password_confirm_hint),
                                fontFamily = RobotoCondensedFontFamily,
                                color = BugListColors.Muted
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (showConfirm) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(
                                    imageVector = if (showConfirm) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = BugListColors.Muted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BugListColors.Gold,
                            unfocusedBorderColor = BugListColors.Divider,
                            focusedTextColor = BugListColors.Platinum,
                            unfocusedTextColor = BugListColors.Platinum,
                            cursorColor = BugListColors.Gold
                        ),
                        isError = mismatch,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Validation / hint text
                when {
                    tooShort -> Text(
                        stringResource(R.string.backup_password_too_short),
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 12.sp,
                        color = BugListColors.DebtRed
                    )
                    mismatch -> Text(
                        stringResource(R.string.backup_password_mismatch),
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 12.sp,
                        color = BugListColors.DebtRed
                    )
                    errorMessage != null -> Text(
                        errorMessage,
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 12.sp,
                        color = BugListColors.DebtRed
                    )
                    isWeak && password.length >= 4 -> Text(
                        stringResource(R.string.backup_password_weak_warning),
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 12.sp,
                        color = BugListColors.Muted
                    )
                }

                // Loading spinner
                if (isLoading) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(0.dp),
                            color = BugListColors.Gold,
                            strokeWidth = 2.dp
                        )
                        Text(
                            if (isExport) stringResource(R.string.backup_exporting)
                            else stringResource(R.string.backup_importing),
                            fontFamily = RobotoCondensedFontFamily,
                            fontSize = 13.sp,
                            color = BugListColors.Muted
                        )
                    }
                }
            }
        },
        confirmButton = {
            Surface(
                onClick = { if (canConfirm) onConfirm(password) },
                shape = RoundedCornerShape(8.dp),
                color = if (canConfirm) BugListColors.Gold else BugListColors.GoldDim
            ) {
                Text(
                    text = stringResource(R.string.action_confirm),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BugListColors.Background,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isLoading) onDismiss() }
            ) {
                Text(
                    stringResource(R.string.action_cancel),
                    fontFamily = RobotoCondensedFontFamily,
                    color = BugListColors.Muted
                )
            }
        }
    )
}

/**
 * Confirmation dialog shown after successful backup decryption.
 * Displays backup contents summary before the user commits to overwriting local data.
 */
@Composable
fun BackupConfirmDialog(
    personCount: Int,
    debtCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BugListColors.Surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = stringResource(R.string.backup_confirm_title),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BugListColors.Gold,
                letterSpacing = 1.sp
            )
        },
        text = {
            Text(
                text = stringResource(R.string.backup_confirm_message, personCount, debtCount),
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 14.sp,
                color = BugListColors.Platinum,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Surface(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp),
                color = BugListColors.DebtRed.copy(alpha = 0.15f),
                modifier = Modifier.border(1.dp, BugListColors.DebtRed, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = stringResource(R.string.backup_confirm_apply),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BugListColors.DebtRed,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_cancel),
                    fontFamily = RobotoCondensedFontFamily,
                    color = BugListColors.Muted
                )
            }
        }
    )
}
