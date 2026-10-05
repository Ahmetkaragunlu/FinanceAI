package com.ahmetkaragunlu.financeai.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DeviceWorkMigration {
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE account_preferences ADD COLUMN timeZoneId TEXT")
            db.execSQL("CREATE TABLE IF NOT EXISTS photo_operations (ownerId TEXT NOT NULL, `collection` TEXT NOT NULL, remoteId TEXT NOT NULL, path TEXT NOT NULL, version TEXT NOT NULL, failure TEXT, PRIMARY KEY(ownerId, `collection`, remoteId, path))")
            db.execSQL("CREATE TABLE IF NOT EXISTS schedule_commands (operationId TEXT NOT NULL PRIMARY KEY, ownerId TEXT NOT NULL, remoteId TEXT NOT NULL, scheduledDate INTEGER NOT NULL, type TEXT NOT NULL, requestedAt INTEGER NOT NULL, planPayload TEXT, planBasePayload TEXT, financialPayload TEXT, failure TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS reminder_state (ownerId TEXT NOT NULL, remoteId TEXT NOT NULL, scheduledDate INTEGER NOT NULL, automaticSlots INTEGER NOT NULL, snoozeAt INTEGER, lastShownAt INTEGER, expiredShownAt INTEGER, deleteAt INTEGER, consumedSnoozeAt INTEGER, revision INTEGER NOT NULL, active INTEGER NOT NULL, PRIMARY KEY(ownerId, remoteId))")
            db.execSQL("CREATE TABLE IF NOT EXISTS push_events (ownerId TEXT NOT NULL, eventId TEXT NOT NULL, remoteId TEXT NOT NULL, type TEXT NOT NULL, receivedAt INTEGER NOT NULL, handled INTEGER NOT NULL, PRIMARY KEY(ownerId, eventId))")
            db.execSQL("CREATE TABLE IF NOT EXISTS token_operations (ownerId TEXT NOT NULL, token TEXT NOT NULL, remove INTEGER NOT NULL, createdAt INTEGER NOT NULL, acknowledged INTEGER NOT NULL, PRIMARY KEY(ownerId, token))")
        }
    }
}
