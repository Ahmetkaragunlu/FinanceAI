package com.ahmetkaragunlu.financeai.app.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceBackupSnapshotTest {
    @Test
    fun backupKeepsCommittedFinancialAndPendingDataButExcludesDeviceQueuesWithoutChangingSource() {
        withSnapshot { source, destination ->
            SQLiteDatabase.openOrCreateDatabase(source, null).use { database ->
                database.enableWriteAheadLogging()
                database.execSQL("PRAGMA user_version = 15")
                for (table in
                    listOf(
                        "transaction_table",
                        "sync_records",
                        "schedule_commands",
                        "account_preferences",
                        "token_operations",
                        "push_events",
                        "active_account",
                    )) {
                    database.execSQL("CREATE TABLE `$table` (value TEXT)")
                    database.execSQL(
                        "INSERT INTO `$table` VALUES (?)",
                        arrayOf("retained-test-value"),
                    )
                }
                FinanceBackupSnapshot.create(source, destination)
                assertEquals(1, count(database, "token_operations"))
                assertEquals(1, count(database, "active_account"))
                assertEquals(1, count(database, "transaction_table"))
            }
            SQLiteDatabase.openDatabase(destination.path, null, SQLiteDatabase.OPEN_READONLY).use {
                restored ->
                assertEquals(15, restored.version)
                for (table in
                    listOf(
                        "transaction_table",
                        "sync_records",
                        "schedule_commands",
                        "account_preferences",
                    )) {
                    assertEquals(1, count(restored, table))
                }
                for (table in
                    listOf("token_operations", "push_events", "active_account")) assertEquals(
                    0,
                    count(restored, table),
                )
            }
        }
    }

    @Test
    fun restoredLocalPhotoPathsFollowTheNewDataDirectoryAndRemoteUrlsRemainUnchanged() {
        withSnapshot { source, destination ->
            SQLiteDatabase.openOrCreateDatabase(source, null).use { database ->
                for ((table, field) in
                    listOf(
                        "transaction_table" to "photoUri",
                        "scheduled_transactions_table" to "photoUri",
                        "photo_operations" to "path",
                    )) {
                    database.execSQL("CREATE TABLE `$table` (`$field` TEXT)")
                    database.execSQL(
                        "INSERT INTO `$table` VALUES (?)",
                        arrayOf("/old/private/files/transaction_photos/account/IMG_test.jpg"),
                    )
                    database.execSQL(
                        "INSERT INTO `$table` VALUES (?)",
                        arrayOf("https://example.com/transaction_photos/remote.jpg"),
                    )
                }
            }
            FinanceBackupSnapshot.create(source, destination)
            val newFiles = File(destination.parentFile, "new-files")
            FinanceBackupSnapshot.rebasePhotos(destination, newFiles)
            SQLiteDatabase.openDatabase(destination.path, null, SQLiteDatabase.OPEN_READONLY).use {
                database ->
                for ((table, field) in
                    listOf(
                        "transaction_table" to "photoUri",
                        "scheduled_transactions_table" to "photoUri",
                        "photo_operations" to "path",
                    )) {
                    database.rawQuery("SELECT `$field` FROM `$table` ORDER BY rowid", null).use {
                        cursor ->
                        cursor.moveToFirst()
                        assertEquals(
                            File(newFiles, "transaction_photos/account/IMG_test.jpg").canonicalPath,
                            cursor.getString(0),
                        )
                        cursor.moveToNext()
                        assertEquals(
                            "https://example.com/transaction_photos/remote.jpg",
                            cursor.getString(0),
                        )
                    }
                }
            }
        }
    }

    private fun count(database: SQLiteDatabase, table: String): Int =
        database.rawQuery("SELECT COUNT(*) FROM `$table`", null).use {
            it.moveToFirst()
            it.getInt(0)
        }

    @Test fun restoreDoesNotRebaseTraversalEscapesOrUnrelatedPrivatePaths() {
        withSnapshot { source, destination ->
            val paths = listOf("/old/files/transaction_photos/../outside/IMG_escape.jpg",
                "/old/files/transaction_photos/account/../../outside/IMG_escape.jpg",
                "/old/files/unrelated/IMG_receipt.jpg", "https://example.test/transaction_photos/receipt.jpg",
                "/old/files/transaction_photos/account/IMG_owned.jpg")
            SQLiteDatabase.openOrCreateDatabase(source, null).use { database ->
                database.execSQL("CREATE TABLE transaction_table (photoUri TEXT)")
                paths.forEach { database.execSQL("INSERT INTO transaction_table VALUES (?)", arrayOf(it)) }
            }
            FinanceBackupSnapshot.create(source, destination)
            val newFiles = File(destination.parentFile, "new-files")
            FinanceBackupSnapshot.rebasePhotos(destination, newFiles)
            SQLiteDatabase.openDatabase(destination.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.rawQuery("SELECT photoUri FROM transaction_table ORDER BY rowid", null).use { cursor ->
                    paths.take(4).forEach { expected -> cursor.moveToNext(); assertEquals(expected, cursor.getString(0)) }
                    cursor.moveToNext()
                    assertEquals(File(newFiles, "transaction_photos/account/IMG_owned.jpg").canonicalPath, cursor.getString(0))
                }
            }
        }
    }

    private inline fun withSnapshot(test: (File, File) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory =
            File(context.cacheDir, "backup-test-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            test(File(directory, "source.db"), File(directory, "snapshot/finance_db"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
