package com.buglist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.dao.PersonDao
import com.buglist.data.local.dao.TagDao
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.entity.DebtEntryTagCrossRef
import com.buglist.data.local.entity.PaymentEntity
import com.buglist.data.local.entity.PersonEntity
import com.buglist.data.local.entity.TagEntity

/**
 * BugList encrypted Room database.
 *
 * Opened via [net.zetetic.database.sqlcipher.SupportOpenHelperFactory] — NOT
 * the deprecated SupportFactory. See L-033 in lessons.md.
 *
 * SQLCipher PRAGMAs are set in the [DatabaseModule] via a
 * RoomDatabase.Callback before the first connection is used:
 *   - PRAGMA cipher_memory_security = ON
 *   - PRAGMA secure_delete = ON
 *   - PRAGMA cipher_use_hmac = ON
 *   - PRAGMA journal_mode = WAL
 *   - PRAGMA cipher_page_size = 16384  (16KB, required for Android 15+ Play Store)
 *
 * Schema version history:
 *   v1 – Initial schema (persons, debt_entries, payments)
 *   v2 – Tag system: tags table + debt_entry_tags join table
 *
 * NEVER use fallbackToDestructiveMigration() in release builds. Every schema
 * change requires an explicit Migration class. See L-030 in lessons.md.
 */
@Database(
    entities = [
        PersonEntity::class,
        DebtEntryEntity::class,
        PaymentEntity::class,
        TagEntity::class,
        DebtEntryTagCrossRef::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun debtEntryDao(): DebtEntryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun tagDao(): TagDao

    companion object {

        /**
         * Migration from v1 (initial schema) to v2 (tag system).
         *
         * Creates the `tags` table and the `debt_entry_tags` join table with
         * foreign key constraints and indices required for efficient tag lookups.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debt_entry_tags` (
                        `debtEntryId` INTEGER NOT NULL,
                        `tagId` INTEGER NOT NULL,
                        PRIMARY KEY(`debtEntryId`, `tagId`),
                        FOREIGN KEY(`debtEntryId`) REFERENCES `debt_entries`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_debt_entry_tags_tagId` ON `debt_entry_tags`(`tagId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_debt_entry_tags_debtEntryId` ON `debt_entry_tags`(`debtEntryId`)"
                )
            }
        }
    }
}
