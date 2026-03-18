package com.buglist.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.SecureRandom

/**
 * Verifies that a SQLCipher database file is genuinely encrypted.
 *
 * An unencrypted SQLite database always starts with the ASCII string
 * "SQLite format 3\000" (16 bytes). A properly encrypted SQLCipher
 * database should NOT contain this header — if it does, encryption failed.
 */
@RunWith(AndroidJUnit4::class)
class EncryptionVerificationTest {

    @Test
    fun encryptedDatabaseDoesNotContainPlaintextSQLiteHeader() {
        System.loadLibrary("sqlcipher")

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.getDatabasePath("encryption_test.db").absolutePath)
        dbFile.parentFile?.mkdirs()

        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }

        try {
            // Create and write to database — this forces the file to be created
            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, passphrase, null, null, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY, value TEXT);")
            db.execSQL("INSERT INTO test_table (value) VALUES ('sensitive_data');")
            db.close()

            // Read raw bytes from the database file
            assertTrue("Database file should exist after creation", dbFile.exists())
            val header = dbFile.readBytes().take(16).toByteArray()

            // SQLite plaintext magic: "SQLite format 3\000"
            val sqliteMagic = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
            val headerMatchesSqlite = header.take(sqliteMagic.size).toByteArray()
                .contentEquals(sqliteMagic)

            assertFalse(
                "Database file must NOT start with SQLite plaintext magic — encryption failed!",
                headerMatchesSqlite
            )

            // Also check that the word "sensitive_data" does not appear in the file
            val fileContents = dbFile.readBytes()
            val sensitiveBytes = "sensitive_data".toByteArray(Charsets.UTF_8)
            val containsPlaintext = fileContents.indices.any { i ->
                i + sensitiveBytes.size <= fileContents.size &&
                        fileContents.slice(i until i + sensitiveBytes.size)
                            .toByteArray().contentEquals(sensitiveBytes)
            }

            assertFalse(
                "Database must NOT contain plaintext 'sensitive_data' — encryption failed!",
                containsPlaintext
            )
        } finally {
            passphrase.fill(0)
            dbFile.delete()
        }
    }

    @Test
    fun encryptedDatabaseCanBeOpenedWithCorrectPassphrase() {
        System.loadLibrary("sqlcipher")

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.getDatabasePath("passphrase_test.db").absolutePath)
        dbFile.parentFile?.mkdirs()

        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }

        try {
            // Create
            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, passphrase, null, null, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS verify (id INTEGER PRIMARY KEY);")
            db.execSQL("INSERT INTO verify VALUES (42);")
            db.close()

            // Re-open with same passphrase — must succeed
            val db2 = SQLiteDatabase.openOrCreateDatabase(dbFile, passphrase, null, null, null)
            val cursor = db2.rawQuery("SELECT id FROM verify", arrayOf())
            assertTrue("Should have one row", cursor.moveToFirst())
            val id = cursor.getInt(0)
            cursor.close()
            db2.close()

            assertTrue("Value should be 42", id == 42)
        } finally {
            passphrase.fill(0)
            dbFile.delete()
        }
    }

    @Test
    fun databaseCannotBeOpenedWithWrongPassphrase() {
        System.loadLibrary("sqlcipher")

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.getDatabasePath("wrong_pass_test.db").absolutePath)
        dbFile.parentFile?.mkdirs()

        val correctPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrongPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }

        try {
            // Create with correct passphrase
            val db = SQLiteDatabase.openOrCreateDatabase(
                dbFile, correctPassphrase, null, null, null
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS data (id INTEGER PRIMARY KEY);")
            db.close()

            // Try to open with wrong passphrase — should throw or return garbage
            var exceptionThrown = false
            try {
                val db2 = SQLiteDatabase.openOrCreateDatabase(
                    dbFile, wrongPassphrase, null, null, null
                )
                // If opened, a query should fail because the DB is garbled
                db2.rawQuery("SELECT * FROM data", arrayOf())
                db2.close()
            } catch (e: Exception) {
                exceptionThrown = true
            }

            assertTrue("Opening with wrong passphrase must throw an exception", exceptionThrown)
        } finally {
            correctPassphrase.fill(0)
            wrongPassphrase.fill(0)
            dbFile.delete()
        }
    }
}
