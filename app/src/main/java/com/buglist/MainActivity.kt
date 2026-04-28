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
 * ## Background behaviour
 * [onUserLeaveHint] is called when the user explicitly navigates away (Home button, Recents).
 * The app immediately locks the session and removes itself from the task stack so there is
 * no thumbnail of sensitive data in the Recents screen. On next launch, a fresh [onCreate]
 * is guaranteed → BiometricPrompt is always shown on every open.
 *
 * [onUserLeaveHint] does NOT fire when the app itself starts another Activity (SAF picker,
 * Share Intent, BiometricPrompt overlay) — only genuine user-initiated backgrounding triggers it.
 *
 * ## SplashScreen (L-075)
 * [installSplashScreen] is called BEFORE [super.onCreate] to comply with the SplashScreen API
 * contract. The keep-on-screen condition holds the splash until [BugListApplication.sqlCipherInitJob]
 * completes, ensuring the SQLCipher native library is mapped before the first frame is drawn.
 *
 * Extends [FragmentActivity] (not [androidx.activity.ComponentActivity]) because
 * [androidx.biometric.BiometricPrompt] requires a [FragmentActivity] host. See L-066.
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
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // SECURITY: FLAG_SECURE must be set BEFORE setContent{}.
        // See L-012 in lessons.md.
        window.setFlags(FLAG_SECURE, FLAG_SECURE)

        // SECURITY: Terminate if a debugger is attached in release builds.
        if (!BuildConfig.DEBUG && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())) {
            Process.killProcess(Process.myPid())
            return
        }

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

    /**
     * Called when the user explicitly leaves the app (Home button, Recents gesture).
     * NOT called when the app itself starts another Activity (SAF, Share Intent, Biometric overlay).
     *
     * Locks the session and removes the task from Recents so sensitive data is never
     * visible in the task switcher. The next launch creates a fresh Activity → BiometricPrompt.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        sessionManager.lock()
        finishAndRemoveTask()
    }
}
