package com.buglist.presentation.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buglist.R
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.BugListTypography
import com.buglist.security.BiometricAuthManager

/**
 * Authentication screen — the first screen shown on every app launch.
 *
 * Uses [auth_background.png] (the splash-screen.png asset) as fullscreen background.
 * App name "BUGLIST" in Oswald Bold Gold, centered. Pulsing fingerprint icon.
 *
 * Automatically triggers BiometricPrompt via [LaunchedEffect] when [AuthUiState.Authenticating].
 * The [activity] parameter is passed explicitly from [MainActivity] — see L-066 in lessons.md.
 *
 * @param viewModel        Injected [AuthViewModel].
 * @param activity         The hosting [FragmentActivity] — passed from MainActivity directly.
 * @param biometricManager Injected [BiometricAuthManager].
 * @param onAuthenticated  Callback when authentication succeeds — navigate to dashboard.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    activity: FragmentActivity,
    biometricManager: BiometricAuthManager,
    onAuthenticated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shouldShowPrompt by viewModel.shouldShowPrompt.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Trigger BiometricPrompt when ViewModel signals it.
    LaunchedEffect(shouldShowPrompt) {
        if (shouldShowPrompt) {
            biometricManager.authenticate(
                activity = activity,
                title = context.getString(R.string.auth_biometric_prompt_title),
                subtitle = context.getString(R.string.auth_biometric_prompt_subtitle),
                negativeButton = context.getString(R.string.auth_biometric_prompt_negative),
                onResult = { result -> viewModel.onAuthResult(result) }
            )
            viewModel.onPromptShown()
        }
    }

    // Navigate to dashboard on success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthenticated()
        }
    }

    // Auto-trigger auth on first composition
    LaunchedEffect(Unit) {
        viewModel.requestAuthentication()
    }

    AuthScreenContent(
        uiState = uiState,
        onRetry = { viewModel.resetForRetry() },
        onTapFingerprint = { viewModel.resetForRetry() }
    )
}

/**
 * Stateless content composable for the auth screen.
 * Separated from [AuthScreen] to enable Compose Preview and testing.
 */
@Composable
fun AuthScreenContent(
    uiState: AuthUiState,
    onRetry: () -> Unit,
    onTapFingerprint: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BugListColors.Background)
    ) {
        val screenHeight = maxHeight

        // Splash background image (auth_background.png from assets/)
        Image(
            painter = painterResource(R.drawable.auth_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.35f,
            modifier = Modifier.fillMaxSize()
        )

        // Dark overlay for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BugListColors.Background.copy(alpha = 0.55f))
        )

        // App name — anchored at ~35% from top, Oswald Bold Gold UPPERCASE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = screenHeight * 0.33f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.auth_title),
                style = BugListTypography.displayLarge,
                color = BugListColors.Gold,
                textAlign = TextAlign.Center
            )
        }

        // Fingerprint icon + status text — anchored so icon centre sits at ~68% of screen height
        // (matches the fingerprint watermark in the background image)
        val isTappable = uiState !is AuthUiState.Authenticating && uiState !is AuthUiState.LockedOut
        val cancelErrorCode = androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
        val statusText = when (uiState) {
            AuthUiState.Idle, AuthUiState.Authenticating ->
                stringResource(R.string.auth_subtitle)
            is AuthUiState.Authenticated ->
                stringResource(R.string.auth_subtitle)
            is AuthUiState.Error -> {
                if (uiState.errorCode == cancelErrorCode) {
                    stringResource(R.string.auth_subtitle)
                } else {
                    uiState.message.uppercase()
                }
            }
            AuthUiState.KeyInvalidated ->
                stringResource(R.string.auth_key_invalidated)
            AuthUiState.LockedOut ->
                stringResource(R.string.auth_error_too_many_attempts)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            // Top of this column sits at ~57% → icon (100dp) centre lands at ~68%
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = screenHeight * 0.57f)
                .padding(horizontal = 32.dp)
        ) {
            // Status text sits just above the fingerprint icon
            Text(
                text = statusText,
                style = BugListTypography.bodyMedium,
                color = when (uiState) {
                    is AuthUiState.Error, AuthUiState.LockedOut -> BugListColors.DebtRed
                    else -> BugListColors.Muted
                },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Animated fingerprint icon — centre aligns with background watermark
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = stringResource(R.string.auth_subtitle),
                tint = when (uiState) {
                    is AuthUiState.Error, AuthUiState.LockedOut -> BugListColors.DebtRed
                    else -> BugListColors.Gold
                },
                modifier = Modifier
                    .size(100.dp)
                    .scale(if (uiState is AuthUiState.Authenticating) scale else 1f)
                    .then(
                        if (isTappable) Modifier.clickable(onClick = onTapFingerprint)
                        else Modifier
                    )
            )

            // Retry button — shown on error (except simple cancel) and KeyInvalidated
            val showRetry = uiState is AuthUiState.KeyInvalidated ||
                (uiState is AuthUiState.Error &&
                    uiState.errorCode != cancelErrorCode)
            if (showRetry) {
                Spacer(modifier = Modifier.height(36.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BugListColors.Gold,
                        contentColor = BugListColors.Background
                    )
                ) {
                    Text(
                        text = stringResource(R.string.auth_retry),
                        style = BugListTypography.labelLarge
                    )
                }
            }
        }
    }
}
