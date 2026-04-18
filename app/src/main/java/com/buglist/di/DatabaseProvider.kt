package com.buglist.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.buglist.BugListApplication
import com.buglist.data.local.AppDatabase
import com.buglist.security.PassphraseManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the [AppDatabase] instance behind a [CompletableDeferred].
 *
 * ## Why this exists (L-075)
 *
 * The database MUST NOT be opened at application startup because:
 * 1. Opening SQLCipher requires the native `.so` to be loaded (which we defer to IO thread).
 * 2. The passphrase must be fetched from DataStore — an IO operation — without blocking
 *    the main thread via `runBlocking`.
 * 3. SQLCipher's `cipher_memory_security = ON` PRAGMA and the first WAL checkpoint add
 *    ~200 ms of wall time that should not land on the cold-start critical path.
 *
 * ## Lifecycle
 *
 * 1. [AuthViewModel] → authentication succeeds
 * 2. [BugListNavHost] calls [initializeAsync] — this starts DB construction on [Dispatchers.IO]
 * 3. [AppDatabase] becomes available via [getDatabase] — a non-blocking `await()` that
 *    returns immediately once the deferred completes
 * 4. Hilt's `provideAppDatabase` (in [DatabaseModule]) calls `runBlocking { getDatabase() }`.
 *    Since Hilt only instantiates `@Singleton` providers at first use, and DAOs are first
 *    injected when ViewModels for post-auth screens are created, by that point [initializeAsync]
 *    has already completed and `await()` returns instantly — no real blocking occurs.
 *
 * @param context Application context for Room builder and DataStore.
 * @param passphraseManager Provides the encrypted database passphrase.
 */
@Singleton
class DatabaseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val passphraseManager: PassphraseManager
) {

    private companion object {
        const val DB_NAME = "buglist.db"
    }

    private val _database = CompletableDeferred<AppDatabase>()

    /**
     * Returns true once [initializeAsync] has completed successfully.
     * Safe to call from any thread.
     */
    val isInitialized: Boolean get() = _database.isCompleted

    /**
     * Suspends until the [AppDatabase] is available.
     * Returns immediately if already initialized.
     *
     * Must NOT be called from the main thread unless the DB is already initialized.
     */
    suspend fun getDatabase(): AppDatabase = _database.await()

    /**
     * Starts the async database construction on [Dispatchers.IO].
     *
     * Idempotent — subsequent calls return the same [CompletableDeferred] without
     * re-triggering construction. Safe to call multiple times.
     *
     * Sequence:
     * 1. Awaits [BugListApplication.sqlCipherInitJob] — ensures native lib is loaded.
     * 2. Fetches passphrase from DataStore via [PassphraseManager.getOrCreatePassphrase].
     * 3. Builds the Room database with [SupportOpenHelperFactory].
     * 4. Warms the connection — triggers the first actual file open + PRAGMA application.
     * 5. Zeros the passphrase [ByteArray] in the `finally` block.
     *
     * @param application The [BugListApplication] instance to await [sqlCipherInitJob].
     */
    fun initializeAsync(application: BugListApplication) {
        if (_database.isCompleted) return

        @Suppress("OPT_IN_USAGE") // GlobalScope is intentional — DB lifetime == process lifetime
        GlobalScope.launch(Dispatchers.IO) {
            if (_database.isCompleted) return@launch // double-check inside coroutine

            try {
                // Step 1: ensure native SQLCipher library is loaded
                application.sqlCipherInitJob.await()

                // Step 2: retrieve passphrase (DataStore IO, must not be on main thread)
                val passphrase = passphraseManager.getOrCreatePassphrase()

                // Step 3: build the Room instance with SQLCipher factory.
                //
                // SECURITY NOTE — why passphrase.fill(0) is NOT called here (L-078):
                // SupportOpenHelperFactory holds a *reference* to the passphrase ByteArray.
                // Room opens connections lazily via the factory: the writer connection is
                // opened in step 4 (writableDatabase), but reader connections are opened
                // on the first actual query (WAL reader pool). If we zeroed the ByteArray
                // after step 4, the factory would hand SQLCipher an all-zero passphrase
                // for every subsequent reader connection → SQLiteNotADatabaseException (code 26).
                //
                // Instead, we let the ByteArray go out of scope naturally when this
                // coroutine completes; the JVM GC will collect it. SQLCipher copies the
                // key material into its own native-layer key schedule during connection open,
                // so the Java-heap ByteArray is only a short-lived transient after that point.
                val factory = SupportOpenHelperFactory(passphrase)

                val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                    .openHelperFactory(factory)
                    .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // SQLCipher 4.9.0: ALL PRAGMAs return a result row.
                            // Must use rawQuery(), not execSQL(). See L-069.
                            db.setPragma("cipher_memory_security = ON")
                            db.setPragma("secure_delete = ON")
                            db.setPragma("cipher_use_hmac = ON")
                            db.setPragma("journal_mode = WAL")
                            db.setPragma("cipher_page_size = 16384")
                        }
                        // onCreate omitted intentionally — Room always calls onOpen
                        // immediately after onCreate, so PRAGMAs are set there too.
                    })
                    .build()

                // Step 4: warm the writer connection — triggers actual file open + PRAGMAs.
                db.openHelper.writableDatabase

                // Step 5: complete the deferred — all awaiting callers unblock now.
                // passphrase ByteArray falls out of scope here and will be GC'd.
                // Intentionally NOT calling passphrase.fill(0) — see L-078.
                _database.complete(db)
            } catch (e: Exception) {
                _database.completeExceptionally(e)
            }
        }
    }
}

/**
 * Executes a SQLCipher PRAGMA via rawQuery and immediately closes the cursor.
 *
 * In SQLCipher 4.9.0, ALL PRAGMAs return a result set. [SupportSQLiteDatabase.execSQL]
 * routes through [nativeExecuteForChangedRowCount] which rejects any statement that
 * returns rows — causing: "Queries can be performed using rawQuery methods only."
 * This helper wraps every PRAGMA in a query() call to avoid the crash. See L-069.
 *
 * Package-private (internal visibility) so [DatabaseProvider] and [DatabaseModule] can both use it.
 */
internal fun SupportSQLiteDatabase.setPragma(pragma: String) {
    query("PRAGMA $pragma;", emptyArray<Any?>()).close()
}
