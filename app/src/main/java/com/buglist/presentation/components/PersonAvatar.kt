package com.buglist.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.presentation.theme.OswaldFontFamily

/**
 * Circular avatar showing the person's initials.
 *
 * If [avatarColor] is 0 (default), a deterministic color is generated from [name].
 * The color is stable for the same name — same person always gets the same color.
 *
 * @param name        Person's display name.
 * @param avatarColor Stored ARGB color int (0 = auto-generate from name).
 * @param size        Circle diameter. Defaults to 48dp.
 * @param modifier    Optional layout modifier.
 */
@Composable
fun PersonAvatar(
    name: String,
    avatarColor: Int = 0,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
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

    // Ensure text is readable — use dark text on light backgrounds, white on dark
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
 */
private fun generateAvatarColor(name: String): Color {
    val palette = listOf(
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
    val index = kotlin.math.abs(name.hashCode()) % palette.size
    return palette[index]
}
