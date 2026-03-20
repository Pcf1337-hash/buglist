package com.buglist.presentation.edit_person

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.domain.model.Person
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.components.PersonAvatar
import com.buglist.presentation.components.avatarColorPalette
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import kotlinx.coroutines.launch

/**
 * Bottom sheet for editing an existing person's details.
 *
 * Differs from [com.buglist.presentation.add_person.AddPersonSheet] in that it:
 *  - Pre-fills all fields from the existing [person].
 *  - Shows an avatar preview with a color picker (10 preset colors + auto).
 *  - Offers a **photo picker** button — the user can set a custom avatar image
 *    from their gallery. The image is copied and compressed to internal storage.
 *  - Offers a **remove photo** button when a custom image is currently set.
 *
 * AddPersonSheet is deliberately untouched — the photo/color picker is exclusive
 * to the edit flow to keep the add flow fast and focused.
 *
 * @param person    The existing [Person] to edit.
 * @param onDismiss Called when the sheet is closed without saving.
 * @param onSaved   Called after the person has been successfully updated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonSheet(
    person: Person,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditPersonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Editable form state — seeded from the existing person.
    var nameInput by rememberSaveable { mutableStateOf(person.name) }
    var phoneInput by rememberSaveable { mutableStateOf(person.phone ?: "") }
    var notesInput by rememberSaveable { mutableStateOf(person.notes ?: "") }
    var selectedColor by rememberSaveable { mutableIntStateOf(person.avatarColor) }
    // null = no image set / cleared; non-null = path of the active avatar photo
    var currentImagePath by rememberSaveable { mutableStateOf(person.avatarImagePath) }

    // System photo picker — PickVisualMedia requires no permission on API 33+,
    // and uses the legacy intent on older devices transparently.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = viewModel.copyImageToInternal(person.id, uri)
                if (path != null) currentImagePath = path
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is EditPersonUiState.Success) {
            viewModel.reset()
            onSaved()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BugListColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ── Title ──────────────────────────────────────────────────────────
            Text(
                text = "PERSON BEARBEITEN",
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BugListColors.Gold
            )
            Spacer(Modifier.height(20.dp))

            // ── Avatar preview + photo controls ───────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large avatar preview — shows photo if set, else initials with selected color
                PersonAvatar(
                    name = nameInput.ifBlank { person.name },
                    avatarColor = selectedColor,
                    size = 80.dp,
                    avatarImagePath = currentImagePath
                )
                Spacer(Modifier.height(12.dp))

                // Photo action buttons row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Foto wählen",
                            tint = BugListColors.Gold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "FOTO WÄHLEN",
                            fontFamily = OswaldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BugListColors.Gold
                        )
                    }
                    if (currentImagePath != null) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.deleteAvatarImage(person.id)
                                currentImagePath = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Foto entfernen",
                                tint = BugListColors.DebtRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "ENTFERNEN",
                                fontFamily = OswaldFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BugListColors.DebtRed
                            )
                        }
                    }
                }
            }

            // ── Color picker ──────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text(
                text = "FARBE",
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = BugListColors.Muted,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "Auto" option — color 0 means name-based generation
                ColorCircle(
                    color = BugListColors.Muted,
                    label = "A",
                    isSelected = selectedColor == 0,
                    onClick = { selectedColor = 0 }
                )
                avatarColorPalette.forEach { paletteColor ->
                    val argb = android.graphics.Color.argb(
                        (paletteColor.alpha * 255).toInt(),
                        (paletteColor.red * 255).toInt(),
                        (paletteColor.green * 255).toInt(),
                        (paletteColor.blue * 255).toInt()
                    )
                    ColorCircle(
                        color = paletteColor,
                        isSelected = selectedColor == argb,
                        onClick = { selectedColor = argb }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Text fields ───────────────────────────────────────────────────
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BugListColors.Gold,
                unfocusedBorderColor = BugListColors.Divider,
                focusedTextColor = BugListColors.Platinum,
                unfocusedTextColor = BugListColors.Platinum,
                cursorColor = BugListColors.Gold
            )
            val fieldShape = RoundedCornerShape(8.dp)

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = {
                    Text("Name *", fontFamily = RobotoCondensedFontFamily, color = BugListColors.Muted)
                },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = {
                    Text("Telefon", fontFamily = RobotoCondensedFontFamily, color = BugListColors.Muted)
                },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = {
                    Text("Notizen", fontFamily = RobotoCondensedFontFamily, color = BugListColors.Muted)
                },
                minLines = 2,
                maxLines = 3,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (uiState is EditPersonUiState.Error) {
                Text(
                    text = (uiState as EditPersonUiState.Error).message,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 13.sp,
                    color = BugListColors.DebtRed
                )
                Spacer(Modifier.height(8.dp))
            }

            GoldButton(
                text = "SPEICHERN",
                onClick = {
                    viewModel.savePerson(
                        person = person,
                        name = nameInput,
                        phone = phoneInput,
                        notes = notesInput,
                        avatarColor = selectedColor,
                        avatarImagePath = currentImagePath
                    )
                },
                enabled = nameInput.isNotBlank() && uiState !is EditPersonUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * A tappable colored circle for the avatar color picker.
 *
 * @param color      The fill color of the circle.
 * @param label      Optional label text shown inside (used for the "Auto" option).
 * @param isSelected Whether this color is currently active — shown with a gold border ring.
 * @param onClick    Called when the circle is tapped.
 */
@Composable
private fun ColorCircle(
    color: Color,
    label: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(2.5.dp, BugListColors.Gold, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        if (label != null) {
            Text(
                text = label,
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
        }
    }
}
