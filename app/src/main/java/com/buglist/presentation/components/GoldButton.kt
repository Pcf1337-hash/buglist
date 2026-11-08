package com.buglist.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily

/**
 * Primary call-to-action button.
 *
 * Cyan fill, near-black text, a soft glow. Oswald Bold UPPERCASE.
 * Disabled state uses [BugListColors.GoldDim].
 */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = BugListColors.Gold,
            contentColor = BugListColors.SurfaceDark,
            disabledContainerColor = BugListColors.GoldDim,
            disabledContentColor = BugListColors.SurfaceDark
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 14.dp else 0.dp,
                shape = shape,
                ambientColor = BugListColors.Gold.copy(alpha = 0.38f),
                spotColor = BugListColors.Gold.copy(alpha = 0.28f)
            )
            .border(1.dp, BugListColors.BorderGold, shape)
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 1.sp
        )
    }
}
