package com.buglist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.buglist.data.local.dao.DebtEntryDao
import com.buglist.data.local.dao.DividerDao
import com.buglist.data.local.dao.PaymentDao
import com.buglist.data.local.dao.PersonDao
import com.buglist.data.local.dao.TagDao
import com.buglist.data.local.entity.DebtEntryEntity
import com.buglist.data.local.entity.DebtEntryTagCrossRef
import com.buglist.data.local.entity.DividerEntity
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
 *   v3 – Manual crew sort: sort_index column added to persons table
 *   v4 – Custom avatar photo: avatarImagePath column (nullable TEXT) added to persons
 *   v5 – Divider separators: new `dividers` table for crew-list section headers
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
        DebtEntryTagCrossRef::class,
        DividerEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun debtEntryDao(): DebtEntryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun tagDao(): TagDao
    abstract fun dividerDao(): DividerDao

    companion object {

        /**
         * Migration from v1 (initial schema) to v2 (tag system).
         *
         * Creates the `tags` table and the `debt_entry_tags` join table with
         * foreign key constraints and indices required for efficient tag lookups.
         */
        /**
         * Migration from v2 to v3: adds the `sort_index` column to persons.
         *
         * DEFAULT 2147483647 = Int.MAX_VALUE — existing persons start as "unsorted"
         * so they continue to appear in alphabetical order (name is the SQL tiebreaker
         * when sort_index values are equal). On first drag-to-reorder by the user
         * all items are renumbered 0, 1, 2, … and the manual order takes effect.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE persons ADD COLUMN sortIndex INTEGER NOT NULL DEFAULT 2147483647"
                )
            }
        }

        /**
         * Migration from v3 to v4: adds nullable `avatarImagePath` column to persons.
         * NULL = no custom photo (existing persons keep initials avatar).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE persons ADD COLUMN avatarImagePath TEXT"
                )
            }
        }

        /**
         * Migration from v4 to v5: creates the `dividers` table.
         *
         * Dividers are decorative separator rows in the crew list. They share the
         * `sortIndex` value space with persons so drag-to-reorder works uniformly.
         * Default sortIndex = 2147483647 (Int.MAX_VALUE) → new dividers appear at
         * the bottom before the user places them via reorder.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dividers` (
                        `id`        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `label`     TEXT NOT NULL,
                        `color`     INTEGER NOT NULL DEFAULT -2240,
                        `lineStyle` TEXT NOT NULL DEFAULT 'SOLID',
                        `sortIndex` INTEGER NOT NULL DEFAULT 2147483647
                    )
                    """.trimIndent()
                )
            }
        }

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
