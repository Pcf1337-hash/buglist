package com.buglist

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

/**
 * BugList Application class.
 *
 * ## Cold-start optimisation (L-075)
 *
 * [System.loadLibrary] for "sqlcipher" is deliberately moved to a
 * [GlobalScope.async] on [Dispatchers.IO]. This lets the main thread proceed
 * immediately to Hilt initialisation and the first frame of the SplashScreen
 * while the native library is mapped into memory in the background.
 *
 * [sqlCipherInitJob] is a public [Deferred] so that [com.buglist.di.DatabaseProvider]
 * can `await()` it before opening the database, ensuring the native lib is ready
 * exactly when it is needed — never earlier, never later.
 *
 * Using [GlobalScope] here is intentional and correct: the lifetime of this job
 * is identical to the process lifetime, which is exactly what [GlobalScope] models.
 * There is no leak risk.
 *
 * See L-064 in lessons.md: System.loadLibrary("sqlcipher") is idempotent — the
 * ClassLoader prevents a real double-load.
 */
@HiltAndroidApp
class BugListApplication : Application() {

    /**
     * Deferred for SQLCipher native library loading. Starts immediately in
     * [onCreate] on the IO dispatcher and completes as soon as the .so is mapped.
     *
     * Consumers must call [sqlCipherInitJob].await() before opening the database.
     */
    lateinit var sqlCipherInitJob: Deferred<Unit>
        private set

    override fun onCreate() {
        super.onCreate()

        // Start native lib load immediately in background — parallel to Hilt init and UI setup.
        // See L-075: moving this off the main thread is the key cold-start optimisation.
        @Suppress("OPT_IN_USAGE") // GlobalScope is intentional for process-lifetime jobs
        sqlCipherInitJob = GlobalScope.async(Dispatchers.IO) {
            System.loadLibrary("sqlcipher")
        }
    }
}
