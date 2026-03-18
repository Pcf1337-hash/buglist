package com.buglist

import android.os.Bundle
import android.os.Debug
import android.os.Process
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.buglist.di.DatabaseProvider
import com.buglist.presentation.BugListNavHost
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.BugListTheme
import com.buglist.security.BiometricAuthManager
import com.buglist.security.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity entry point for BugList.
 *
 * Security invariants enforced in [onCreate]:
 * 1. [FLAG_SECURE] — set BEFORE [setContent] so every frame (including splash) is protected.
 *    Prevents screenshots, screen recordings, and Recent Apps thumbnails. See L-012.
 * 2. Debugger detection — in release builds, a connected debugger terminates the process
 *    immediately. An attacker cannot pause execution to inspect memory or bypass biometrics.
 *
 * ## SplashScreen (L-075)
 * [installSplashScreen] is called BEFORE [super.onCreate] to comply with the SplashScreen API
 * contract. The keep-on-screen condition holds the splash until [BugListApplication.sqlCipherInitJob]
 * completes, ensuring the SQLCipher native library is mapped before the first frame is drawn.
 * This replaces the old `System.loadLibrary` on the main thread with a background job that starts
 * the moment the process is created — the splash hides any perceptible delay.
 *
 * Extends [FragmentActivity] (not [androidx.activity.ComponentActivity]) because
 * [androidx.biometric.BiometricPrompt] requires a [FragmentActivity] host. [FragmentActivity]
 * extends [ComponentActivity], so all Compose and Hilt APIs still work. See L-066.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var databaseProvider: DatabaseProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen API: installSplashScreen MUST be called before super.onCreate().
        // It installs the splash theme and registers the exit animation callback.
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // SECURITY: FLAG_SECURE must be set BEFORE setContent{}.
        // See L-012 in lessons.md.
        window.setFlags(FLAG_SECURE, FLAG_SECURE)

        // SECURITY: Terminate if a debugger is attached in release builds.
        // BuildConfig.DEBUG is false in release — R8 will constant-fold this away entirely.
        if (!BuildConfig.DEBUG && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())) {
            Process.killProcess(Process.myPid())
            return
        }

        // Keep the SplashScreen visible until the SQLCipher native library has finished loading.
        // sqlCipherInitJob starts in Application.onCreate() on Dispatchers.IO.
        // Typical load time: 50–150 ms on real hardware — barely perceptible.
        // isCompleted is thread-safe (Deferred backed by atomic state).
        val app = application as BugListApplication
        splashScreen.setKeepOnScreenCondition { !app.sqlCipherInitJob.isCompleted }

        enableEdgeToEdge()

        setContent {
            BugListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BugListColors.Background
                ) {
                    BugListNavHost(
                        activity = this@MainActivity,
                        biometricManager = biometricAuthManager,
                        sessionManager = sessionManager,
                        databaseProvider = databaseProvider
                    )
                }
            }
        }
    }
}
