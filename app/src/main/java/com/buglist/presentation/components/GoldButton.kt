package com.buglist.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily

/**
 * Primary call-to-action button.
 *
 * Cyan fill, near-black text, a light rim. No blur: elevation shadows use a
 * RenderEffect and have stalled the emulator RenderThread.
 * Disabled state uses [BugListColors.GoldDim].
 */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val shape = RoundedCornerShape(20.dp)
    val fill = if (emphasized) Color(0xFFFFD35A) else BugListColors.Gold
    val rim = if (emphasized) Color(0xFFFFF0BD) else BugListColors.BorderGold
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = fill,
            contentColor = BugListColors.SurfaceDark,
            disabledContainerColor = if (emphasized) BugListColors.SurfaceOverlay else BugListColors.GoldDim,
            disabledContentColor = if (emphasized) BugListColors.TextSecondary else BugListColors.SurfaceDark
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (emphasized) 64.dp else 56.dp)
            .border(if (emphasized) 2.dp else 1.dp, if (enabled) rim else BugListColors.BorderSubtle, shape)
    ) {
        if (emphasized) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text.uppercase(),
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (emphasized) 18.sp else 16.sp,
            letterSpacing = 1.sp
        )
    }
}
