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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.eventFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.buglist.R
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.BugListTypography
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
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

    val lifecycleOwner = LocalLifecycleOwner.current

    // ── CASE A: AuthScreen enters composition AFTER Activity.onResume has already fired ──
    //
    // This is the background-return scenario:
    //   1. User is on Dashboard, app goes to background
    //   2. SessionManager.onStart() fires via ProcessLifecycleOwner → lock() → isAuthenticated=false
    //   3. LaunchedEffect(isAuthenticated) in BugListNavHost fires → navigates to Routes.AUTH
    //   4. AuthScreen enters composition — but Activity is ALREADY in RESUMED state
    //   5. LifecycleResumeEffect (Case B) won't fire because ON_RESUME event already happened
    //      before this composable existed in the composition tree
    //
    // Fix: lifecycle.withResumed{} completes IMMEDIATELY when lifecycle >= RESUMED,
    // or suspends until the next ON_RESUME if not yet resumed (handles cold-start too).
    // The idempotency guard in AuthViewModel.requestAuthentication() prevents double-firing
    // if both Case A and Case B trigger at the same time (e.g. on cold-start).
    LaunchedEffect(Unit) {
        // If the Activity is not yet RESUMED (cold start — composable runs during onStart),
        // wait for the next ON_RESUME event. If already RESUMED (background-return via
        // navigation — LaunchedEffect in NavHost fires after Activity.onResume), skip straight
        // to the delay. Either way, requestAuthentication() is called exactly once per entry.
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleOwner.lifecycle.eventFlow.first { it == Lifecycle.Event.ON_RESUME }
        }
        delay(250L) // avoid "authenticate() called after onSaveInstanceState()" on OEM ROMs
        viewModel.requestAuthentication()
    }

    // ── CASE B: AuthScreen is ALREADY in composition, user backgrounds + returns ──
    //
    // Example: user cancels biometrics, then backgrounds the app, then returns.
    // LifecycleResumeEffect fires on each ON_RESUME AFTER the composable has entered composition.
    // The handler reference is stored so pending runnables can be cancelled on pause,
    // preventing a stale requestAuthentication() firing after cancelAuthentication().
    LifecycleResumeEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = Runnable { viewModel.requestAuthentication() }
        handler.postDelayed(runnable, 250L)

        onPauseOrDispose {
            handler.removeCallbacks(runnable) // cancel pending delayed call before resetting state
            viewModel.cancelAuthentication()
        }
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
            .background(BugListColors.SurfaceDark)
    ) {
        val screenHeight = maxHeight

        // Splash background image (auth_background.png from assets/)
        Image(
            painter = painterResource(R.drawable.auth_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.30f,
            modifier = Modifier.fillMaxSize()
        )

        // Dark overlay for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BugListColors.SurfaceDark.copy(alpha = 0.60f))
        )

        // App name — anchored at ~33% from top, Oswald ExtraBold Gold UPPERCASE
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = screenHeight * 0.30f)
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_title),
                style = BugListTypography.displayLarge.copy(
                    letterSpacing = 2.sp
                ),
                color = BugListColors.Gold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Gold separator line under title
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(1.dp)
                    .background(BugListColors.Gold.copy(alpha = 0.7f))
            )
        }

        // Fingerprint icon + status text — anchored so icon centre sits at ~68% of screen height
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

        val isError = uiState is AuthUiState.Error || uiState is AuthUiState.LockedOut
        val iconTint = if (isError) BugListColors.DebtRed else BugListColors.Gold

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = screenHeight * 0.54f)
                .padding(horizontal = 32.dp)
        ) {
            // Gold glow ring around fingerprint icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(112.dp)
                    .then(
                        if (!isError) Modifier.background(
                            BugListColors.GoldGlow,
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) else Modifier
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = stringResource(R.string.auth_subtitle),
                    tint = iconTint,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(if (uiState is AuthUiState.Authenticating) scale else 1f)
                        .then(
                            if (isTappable) Modifier.clickable(onClick = onTapFingerprint)
                            else Modifier
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status text below icon
            Text(
                text = statusText,
                style = BugListTypography.bodyMedium,
                color = if (isError) BugListColors.DebtRed else BugListColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            // Retry button — shown on error (except simple cancel) and KeyInvalidated
            val showRetry = uiState is AuthUiState.KeyInvalidated ||
                (uiState is AuthUiState.Error &&
                    uiState.errorCode != cancelErrorCode)
            if (showRetry) {
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BugListColors.Gold,
                        contentColor = BugListColors.SurfaceDark
                    )
                ) {
                    Text(
                        text = stringResource(R.string.auth_retry),
                        style = BugListTypography.labelLarge
                    )
                }
            }

            // Fallback CTA — visible in every non-active state so the user always has an
            // explicit way to trigger the BiometricPrompt if it did not appear automatically
            // (e.g. OEM race condition, LockedOut recovery, or first-install edge case).
            // Not shown while Authenticating (prompt is already visible) or after
            // Authenticated (nav to dashboard is imminent).
            val showTapPrompt = uiState !is AuthUiState.Authenticating
                && uiState !is AuthUiState.Authenticated
            if (showTapPrompt) {
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onTapFingerprint) {
                    Text(
                        text = stringResource(R.string.auth_tap_to_unlock),
                        fontFamily = OswaldFontFamily,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        color = BugListColors.Gold.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // App version at bottom
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "v${com.buglist.BuildConfig.VERSION_NAME}",
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 11.sp,
                color = BugListColors.TextMuted
            )
        }
    }
}
