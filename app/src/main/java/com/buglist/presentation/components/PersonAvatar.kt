package com.buglist.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.presentation.theme.OswaldFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Circular avatar for a person.
 *
 * If [avatarImagePath] points to a valid file, that photo is shown cropped to a circle.
 * Otherwise falls back to an initials circle: [avatarColor] is used as background,
 * or a deterministic palette color derived from [name] if [avatarColor] is 0.
 *
 * @param name            Person's display name (used for initials and color generation).
 * @param avatarColor     ARGB color int for the initials background (0 = auto from name).
 * @param size            Circle diameter. Defaults to 48dp.
 * @param avatarImagePath Absolute path to a custom photo. Null = show initials.
 * @param modifier        Optional layout modifier.
 */
@Composable
fun PersonAvatar(
    name: String,
    avatarColor: Int = 0,
    size: Dp = 48.dp,
    avatarImagePath: String? = null,
    modifier: Modifier = Modifier
) {
    // Load bitmap asynchronously from local file to avoid blocking composition.
    var loadedBitmap by remember(avatarImagePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(avatarImagePath) {
        loadedBitmap = if (avatarImagePath != null) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(avatarImagePath)
                    if (file.exists()) BitmapFactory.decodeFile(avatarImagePath) else null
                } catch (_: Exception) {
                    null
                }
            }
        } else null
    }

    val bitmap = loadedBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        InitialsAvatar(name = name, avatarColor = avatarColor, size = size, modifier = modifier)
    }
}

/**
 * Fallback avatar showing two-letter initials on a colored circle.
 */
@Composable
private fun InitialsAvatar(
    name: String,
    avatarColor: Int,
    size: Dp,
    modifier: Modifier
) {
    val initials = remember(name) {
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }
    }

    val bgColor = remember(name, avatarColor) {
        if (avatarColor != 0) Color(avatarColor) else generateAvatarColor(name)
    }

    val textColor = remember(bgColor) {
        val luminance = 0.299f * bgColor.red + 0.587f * bgColor.green + 0.114f * bgColor.blue
        if (luminance > 0.5f) Color(0xFF0D0D0D) else Color.White
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
    ) {
        Text(
            text = initials,
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
            color = textColor
        )
    }
}

/**
 * Generates a deterministic accent color from a name string.
 * Uses a set of streetstyle-appropriate colors.
 * The color palette is also exposed as [avatarColorPalette] for the color picker.
 */
private fun generateAvatarColor(name: String): Color {
    val index = kotlin.math.abs(name.hashCode()) % avatarColorPalette.size
    return avatarColorPalette[index]
}

/**
 * The full avatar color palette — 10 streetstyle accent colors.
 * Used both for auto-color generation and for the color picker in EditPersonSheet.
 */
val avatarColorPalette: List<Color> = listOf(
    Color(0xFFE53935), // Red
    Color(0xFF8E24AA), // Purple
    Color(0xFF1E88E5), // Blue
    Color(0xFF00ACC1), // Cyan
    Color(0xFF43A047), // Green
    Color(0xFFFB8C00), // Orange
    Color(0xFF6D4C41), // Brown
    Color(0xFF3949AB), // Indigo
    Color(0xFF00897B), // Teal
    Color(0xFFD81B60), // Pink
)
