package com.buglist.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily

/**
 * Primary call-to-action button.
 *
 * Gold background, black text, Oswald Bold UPPERCASE.
 * Disabled state uses [BugListColors.GoldDim].
 *
 * @param text      Button label — shown in uppercase automatically.
 * @param onClick   Click handler.
 * @param enabled   Whether the button is interactive. Defaults to true.
 * @param modifier  Optional layout modifier.
 */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BugListColors.Gold,
            contentColor = BugListColors.Background,
            disabledContainerColor = BugListColors.GoldDim,
            disabledContentColor = BugListColors.Background
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
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
