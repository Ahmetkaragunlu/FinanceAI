package com.ahmetkaragunlu.financeai.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AccountMigration {
    /** Unowned development rows are retained, never assigned to whoever signs in first. */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val tables = mapOf(
                "transaction_table" to "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `firestoreId` TEXT NOT NULL, `transaction` TEXT NOT NULL, `note` TEXT NOT NULL, `date` INTEGER NOT NULL, `category` TEXT NOT NULL, `photoUri` TEXT, `locationFull` TEXT, `locationShort` TEXT, `latitude` REAL, `longitude` REAL, `syncedToFirebase` INTEGER NOT NULL",
                "budget_table" to "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `firestoreId` TEXT NOT NULL, `budgetType` TEXT NOT NULL, `category` TEXT, `limitPercentage` REAL, `syncedToFirebase` INTEGER NOT NULL",
                "scheduled_transactions_table" to "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `firestoreId` TEXT NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `note` TEXT, `scheduledDate` INTEGER NOT NULL, `expirationNotificationSent` INTEGER NOT NULL, `notificationSent` INTEGER NOT NULL, `photoUri` TEXT, `locationFull` TEXT, `locationShort` TEXT, `latitude` REAL, `longitude` REAL, `syncedToFirebase` INTEGER NOT NULL"
            )
            tables.forEach { (table, columns) ->
                db.execSQL("CREATE TABLE `legacy_v13_$table` AS SELECT * FROM `$table`")
                db.execSQL("CREATE TABLE `${table}_new` ($columns, `amountMinor` INTEGER NOT NULL, `ownerId` TEXT NOT NULL, `currencyCode` TEXT NOT NULL)")
                val names = mutableListOf<String>()
                db.query("PRAGMA table_info(`$table`)").use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        if (name != "amount") names.add("`$name`")
                    }
                }
                val projection = names.joinToString(",")
                db.execSQL("INSERT INTO `${table}_new` ($projection, amountMinor, ownerId, currencyCode) SELECT $projection, CAST(ROUND(amount) AS INTEGER), '', 'XXX' FROM `$table`")
                db.execSQL("DROP TABLE `$table`")
                db.execSQL("ALTER TABLE `${table}_new` RENAME TO `$table`")
                db.execSQL("UPDATE `$table` SET firestoreId = 'legacy_' || id WHERE ownerId = ''")
                db.execSQL("CREATE UNIQUE INDEX `index_${table}_ownerId_firestoreId` ON `$table` (ownerId, firestoreId)")
            }
            db.execSQL("CREATE TABLE legacy_v13_ai_messages AS SELECT * FROM ai_messages")
            db.execSQL("ALTER TABLE ai_messages ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE ai_messages SET firebaseId = 'legacy_' || id WHERE ownerId = '' AND firebaseId IS NOT NULL")
            db.execSQL("CREATE UNIQUE INDEX index_ai_messages_ownerId_firebaseId ON ai_messages (ownerId, firebaseId)")
            db.execSQL("CREATE TABLE account_preferences (ownerId TEXT NOT NULL PRIMARY KEY, currencyCode TEXT NOT NULL)")
            db.execSQL("CREATE TABLE active_account (id INTEGER NOT NULL PRIMARY KEY, ownerId TEXT NOT NULL)")
            db.execSQL("CREATE TABLE sync_records (ownerId TEXT NOT NULL, collection TEXT NOT NULL, remoteId TEXT NOT NULL, basePayload TEXT, baseRevision INTEGER NOT NULL, pendingPayload TEXT, pendingDelete INTEGER NOT NULL, mutationId TEXT, conflictPayload TEXT, conflictRevision INTEGER, permanentFailure INTEGER NOT NULL, PRIMARY KEY(ownerId,collection,remoteId))")
        }
    }
}
