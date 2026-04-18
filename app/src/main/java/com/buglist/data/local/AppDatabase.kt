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
 *   v6 – Tag dedup fix: UNIQUE index on tags.name; existing duplicate tag rows removed
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
    version = 6,
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

        /**
         * Migration from v5 to v6: adds a UNIQUE index on `tags.name` and deduplicates
         * any duplicate tag rows created by repeated imports before this fix.
         *
         * **Root cause of the duplication bug:**
         * The original `tags` table (created in MIGRATION_1_2) had NO unique constraint on
         * `name`. [TagDao.insertTag] uses `OnConflictStrategy.IGNORE`, which silently
         * ignores constraint violations — but with no constraint, it never fires and always
         * inserts a new row. Every import call thus created a phantom duplicate tag row.
         *
         * **What this migration does:**
         * 1. Creates `tags_new` with a UNIQUE index on `name`.
         * 2. Inserts only the canonical (lowest id) row per name from the old table.
         * 3. Saves cross-ref data to a temp table, remapping duplicate tagIds to canonical ids.
         * 4. Drops old `debt_entry_tags` and `tags`.
         * 5. Renames `tags_new` → `tags`.
         * 6. Recreates `debt_entry_tags` with FK to the renamed table.
         * 7. Re-inserts deduplicated cross-refs from the temp table.
         *
         * All PRAGMAs use `query()` instead of `execSQL()` per L-069 (SQLCipher 4.9.0).
         * FK enforcement is disabled for the duration to allow safe table drops/renames.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // FK enforcement off for the duration — tables are being recreated.
                database.query("PRAGMA foreign_keys = OFF", emptyArray<Any?>()).close()

                // 1. New tags table with UNIQUE index on name.
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tags_new` (
                        `id`        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name`      TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags_new`(`name`)"
                )

                // 2. Canonical tags: keep the row with the lowest id for each unique name.
                database.execSQL(
                    """
                    INSERT INTO `tags_new` (`id`, `name`, `createdAt`)
                    SELECT MIN(id) AS id, name, MIN(createdAt) AS createdAt
                    FROM `tags`
                    GROUP BY name
                    """.trimIndent()
                )

                // 3. Temp table to hold cross-ref pairs remapped to canonical ids.
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `det_temp` " +
                    "(`debtEntryId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL)"
                )
                database.execSQL(
                    """
                    INSERT INTO `det_temp` (`debtEntryId`, `tagId`)
                    SELECT DISTINCT det.debtEntryId,
                        (SELECT MIN(t2.id) FROM `tags` t2
                         WHERE t2.name = t.name) AS canonical_id
                    FROM `debt_entry_tags` det
                    JOIN `tags` t ON t.id = det.tagId
                    """.trimIndent()
                )

                // 4. Drop old tables.
                database.execSQL("DROP TABLE IF EXISTS `debt_entry_tags`")
                database.execSQL("DROP TABLE IF EXISTS `tags`")

                // 5. Rename tags_new → tags.
                database.execSQL("ALTER TABLE `tags_new` RENAME TO `tags`")

                // 6. Recreate debt_entry_tags with FK referencing the renamed `tags` table.
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debt_entry_tags` (
                        `debtEntryId` INTEGER NOT NULL,
                        `tagId`       INTEGER NOT NULL,
                        PRIMARY KEY(`debtEntryId`, `tagId`),
                        FOREIGN KEY(`debtEntryId`) REFERENCES `debt_entries`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`tagId`)       REFERENCES `tags`(`id`)         ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // 7. Insert canonical cross-refs (OR IGNORE to guard against any edge cases).
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `debt_entry_tags` (`debtEntryId`, `tagId`)
                    SELECT DISTINCT `debtEntryId`, `tagId` FROM `det_temp`
                    """.trimIndent()
                )

                // 8. Drop temp table.
                database.execSQL("DROP TABLE IF EXISTS `det_temp`")

                // 9. Recreate indices on debt_entry_tags.
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_debt_entry_tags_tagId` " +
                    "ON `debt_entry_tags`(`tagId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_debt_entry_tags_debtEntryId` " +
                    "ON `debt_entry_tags`(`debtEntryId`)"
                )

                // Re-enable FK enforcement.
                database.query("PRAGMA foreign_keys = ON", emptyArray<Any?>()).close()
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
