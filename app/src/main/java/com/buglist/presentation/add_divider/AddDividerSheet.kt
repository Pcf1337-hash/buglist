package com.buglist.presentation.add_divider

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.domain.model.Divider
import com.buglist.domain.model.DividerLineStyle
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.dashboard.DividerRow
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily

/** Preset color palette for divider lines and labels. */
private val PRESET_COLORS = listOf(
    0xFFFFD700.toInt(), // Gold
    0xFFE5E4E2.toInt(), // Platinum
    0xFFFF3B3B.toInt(), // DebtRed
    0xFF00E676.toInt(), // DebtGreen
    0xFF666666.toInt(), // Muted
    0xFFFFFFFF.toInt(), // White
    0xFFFF9500.toInt(), // Orange
    0xFF5856D6.toInt(), // Purple
    0xFF007AFF.toInt(), // Blue
    0xFFFF2D55.toInt(), // Pink
)

/**
 * Bottom sheet for creating a new divider separator in the crew list.
 *
 * Lets the user choose:
 * - An optional label text
 * - A color from a preset palette
 * - A line style (SOLID / DASHED / THICK)
 *
 * A live preview updates in real time as the user changes settings.
 *
 * @param onDismiss Called when the sheet is closed without saving.
 * @param onSaved   Called after a successful save with the new divider's ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDividerSheet(
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AddDividerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var label     by rememberSaveable { mutableStateOf("") }
    var color     by rememberSaveable { mutableIntStateOf(PRESET_COLORS[0]) }
    var lineStyle by rememberSaveable { mutableStateOf(DividerLineStyle.SOLID) }

    LaunchedEffect(uiState) {
        if (uiState is AddDividerUiState.Success) {
            viewModel.reset()
            onSaved((uiState as AddDividerUiState.Success).dividerId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BugListColors.Surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

            // ── Title ────────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.add_divider_title),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BugListColors.Gold
            )
            Spacer(Modifier.height(20.dp))

            // ── Live Preview ─────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.add_divider_preview_label),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = BugListColors.Muted,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BugListColors.SurfaceHigh)
                    .padding(vertical = 4.dp)
            ) {
                DividerRow(
                    divider = Divider(
                        label = label,
                        color = color,
                        lineStyle = lineStyle
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Label input ──────────────────────────────────────────────────
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = BugListColors.Gold,
                unfocusedBorderColor = BugListColors.Divider,
                focusedTextColor     = BugListColors.Platinum,
                unfocusedTextColor   = BugListColors.Platinum,
                cursorColor          = BugListColors.Gold
            )
            OutlinedTextField(
                value = label,
                onValueChange = { if (it.length <= 40) label = it },
                label = {
                    Text(
                        stringResource(R.string.add_divider_label_hint),
                        fontFamily = RobotoCondensedFontFamily,
                        color = BugListColors.Muted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // ── Color picker ─────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.add_divider_color_label),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = BugListColors.Muted,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(PRESET_COLORS) { presetColor ->
                    ColorSwatch(
                        color = Color(presetColor),
                        selected = color == presetColor,
                        onClick = { color = presetColor }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Line style picker ────────────────────────────────────────────
            Text(
                text = stringResource(R.string.add_divider_style_label),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = BugListColors.Muted,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DividerLineStyle.entries.forEach { style ->
                    val label = when (style) {
                        DividerLineStyle.SOLID  -> stringResource(R.string.divider_style_solid)
                        DividerLineStyle.DASHED -> stringResource(R.string.divider_style_dashed)
                        DividerLineStyle.THICK  -> stringResource(R.string.divider_style_thick)
                    }
                    FilterChip(
                        selected = lineStyle == style,
                        onClick = { lineStyle = style },
                        label = {
                            Text(
                                text = label,
                                fontFamily = OswaldFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor      = BugListColors.Gold,
                            selectedLabelColor          = BugListColors.Background,
                            containerColor              = BugListColors.SurfaceHigh,
                            labelColor                  = BugListColors.Platinum
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled                    = true,
                            selected                   = lineStyle == style,
                            selectedBorderColor        = BugListColors.Gold,
                            borderColor                = BugListColors.Divider
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Error message ────────────────────────────────────────────────
            if (uiState is AddDividerUiState.Error) {
                Text(
                    text = (uiState as AddDividerUiState.Error).message,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 13.sp,
                    color = BugListColors.DebtRed
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Save button ──────────────────────────────────────────────────
            GoldButton(
                text = stringResource(R.string.action_save),
                onClick = { viewModel.saveDivider(label, color, lineStyle) },
                enabled = uiState !is AddDividerUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Circular color swatch button with a check-mark overlay when [selected].
 */
@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(2.dp, BugListColors.Platinum, CircleShape)
                else Modifier.border(1.dp, BugListColors.Divider, CircleShape)
            )
            .clickable(onClick = onClick)
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Returns the perceived luminance (0.0 = black, 1.0 = white). */
private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
