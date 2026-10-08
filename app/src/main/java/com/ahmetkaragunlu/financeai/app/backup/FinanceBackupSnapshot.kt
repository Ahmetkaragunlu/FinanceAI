package com.ahmetkaragunlu.financeai.app.backup

import android.database.sqlite.SQLiteDatabase
import com.ahmetkaragunlu.financeai.core.media.local.PhotoFiles
import java.io.File

/** A consistent copy of Room data with session/device queues removed only from the backup. */
internal object FinanceBackupSnapshot {
    fun create(source: File, destination: File) {
        require(source.canonicalFile != destination.canonicalFile)
        check(
            destination.parentFile?.isDirectory == true || destination.parentFile?.mkdirs() == true
        )
        check(!destination.exists() || destination.delete())
        SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READONLY).use { database
            ->
            // Includes committed WAL records without modifying the live database.
            database.execSQL("VACUUM INTO ?", arrayOf(destination.path))
        }
        SQLiteDatabase.openDatabase(destination.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            database ->
            database.beginTransaction()
            try {
                for (table in listOf("token_operations", "push_events", "active_account")) {
                    if (database.hasTable(table)) database.execSQL("DELETE FROM `$table`")
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
    }

    /**
     * Rebase app-private photo paths when restoring into a different Android user/data directory.
     */
    fun rebasePhotos(databaseFile: File, filesDirectory: File) {
        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            database ->
            database.beginTransaction()
            try {
                for ((table, field) in
                    listOf(
                        "transaction_table" to "photoUri",
                        "scheduled_transactions_table" to "photoUri",
                        "photo_operations" to "path",
                    )) {
                    if (!database.hasTable(table)) continue
                    val paths =
                        database
                            .rawQuery(
                                "SELECT DISTINCT `$field` FROM `$table` WHERE `$field` IS NOT NULL",
                                null,
                            )
                            .use { cursor ->
                                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
                            }
                    for (path in paths) {
                        if (!path.startsWith('/')) continue
                        val marker = path.indexOf("/${PhotoFiles.DIRECTORY}/")
                        if (marker < 0) continue
                        val relative = path.substring(marker + 1)
                        val target = File(filesDirectory, relative).canonicalFile
                        if (
                            !target.path.startsWith(
                                File(filesDirectory, PhotoFiles.DIRECTORY).canonicalPath +
                                    File.separator
                            )
                        )
                            continue
                        database.execSQL(
                            "UPDATE `$table` SET `$field` = ? WHERE `$field` = ?",
                            arrayOf(target.path, path),
                        )
                    }
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
    }

    private fun SQLiteDatabase.hasTable(name: String): Boolean =
        rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name))
            .use { it.moveToFirst() }
}
