package com.buglist.di

import com.buglist.data.local.AppDatabase
import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.DividerDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.dao.PersonDao
import com.buglist.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and DAOs.
 *
 * ## Cold-start architecture (L-075)
 *
 * The actual database construction is delegated to [DatabaseProvider], which opens
 * the database asynchronously on [kotlinx.coroutines.Dispatchers.IO] after biometric
 * authentication succeeds. This removes the `System.loadLibrary` + DataStore read + SQLCipher
 * init sequence from the application cold-start critical path.
 *
 * [provideAppDatabase] uses `runBlocking { databaseProvider.getDatabase() }` to bridge from
 * Hilt's synchronous provider system to the async [DatabaseProvider]. This is safe because:
 * - Hilt's `@Singleton` providers are initialized lazily — only on first injection request.
 * - DAOs are first injected when a post-auth ViewModel is created (Dashboard, PersonDetail, etc.).
 * - [BugListNavHost] calls [DatabaseProvider.initializeAsync] before navigating to Dashboard.
 * - By the time any DAO is requested, [DatabaseProvider] has had auth-roundtrip-time (~300–500 ms)
 *   to complete the IO-bound DB construction — `await()` returns instantly in the happy path.
 *
 * SQLCipher configuration (all mandatory PRAGMAs):
 * - cipher_memory_security = ON  (memory is zeroed after use)
 * - secure_delete = ON           (pages overwritten with zeros on delete)
 * - cipher_use_hmac = ON         (HMAC for page authentication)
 * - journal_mode = WAL           (Write-Ahead Logging for performance)
 * - cipher_page_size = 16384     (16 KB pages — required for Android 15+ Play Store)
 *
 * PRAGMAs are applied in [DatabaseProvider]'s [RoomDatabase.Callback.onOpen] using the
 * [setPragma] extension from DatabaseProvider.kt. See L-069.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the [AppDatabase] singleton.
     *
     * Blocks the calling thread until [DatabaseProvider] completes DB construction.
     * In practice, [DatabaseProvider.initializeAsync] is started at auth success,
     * so by the time this provider is first called (first post-auth DAO injection),
     * the deferred is already complete and `await()` returns immediately.
     *
     * If [DatabaseProvider.initializeAsync] was never called, this will throw
     * [IllegalStateException] to fail fast and surface the programming error.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        databaseProvider: DatabaseProvider
    ): AppDatabase {
        // runBlocking bridges Hilt's synchronous @Provides world to the async DatabaseProvider.
        // In the happy path (initializeAsync called at auth success), the deferred is already
        // complete here and await() returns instantly without blocking any thread.
        return runBlocking { databaseProvider.getDatabase() }
    }

    @Provides
    fun providePersonDao(db: AppDatabase): PersonDao = db.personDao()

    @Provides
    fun provideDebtEntryDao(db: AppDatabase): DebtEntryDao = db.debtEntryDao()

    @Provides
    fun providePaymentDao(db: AppDatabase): PaymentDao = db.paymentDao()

    @Provides
    @Singleton
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun provideDividerDao(db: AppDatabase): DividerDao = db.dividerDao()
}
