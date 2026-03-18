package com.buglist.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buglist.R
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.BugListTypography
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Phase 1 placeholder for the authentication screen.
 *
 * Shows the app branding and fingerprint icon. The full BiometricPrompt
 * integration is implemented in Task 1.4.
 */
@Composable
fun AuthScreenPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BugListColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.auth_title),
                style = BugListTypography.displayLarge,
                color = BugListColors.Gold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Authenticate",
                tint = BugListColors.Gold,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.auth_subtitle),
                style = BugListTypography.bodyLarge,
                color = BugListColors.Muted,
                textAlign = TextAlign.Center
            )
        }
    }
}
