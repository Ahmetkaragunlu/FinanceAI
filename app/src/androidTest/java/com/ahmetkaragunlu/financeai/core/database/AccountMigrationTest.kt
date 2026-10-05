package com.ahmetkaragunlu.financeai.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountMigrationTest {
    @Test fun version13MigrationValidatesRoomSchemaAndRetainsUnownedDevelopmentData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE transaction_table (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, firestoreId TEXT NOT NULL, amount REAL NOT NULL, `transaction` TEXT NOT NULL, note TEXT NOT NULL, date INTEGER NOT NULL, category TEXT NOT NULL, photoUri TEXT, locationFull TEXT, locationShort TEXT, latitude REAL, longitude REAL, syncedToFirebase INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE budget_table (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, firestoreId TEXT NOT NULL, budgetType TEXT NOT NULL, category TEXT, amount REAL NOT NULL, limitPercentage REAL, syncedToFirebase INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE scheduled_transactions_table (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, firestoreId TEXT NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, note TEXT, scheduledDate INTEGER NOT NULL, expirationNotificationSent INTEGER NOT NULL, notificationSent INTEGER NOT NULL, photoUri TEXT, locationFull TEXT, locationShort TEXT, latitude REAL, longitude REAL, syncedToFirebase INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE ai_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, text TEXT NOT NULL, isAi INTEGER NOT NULL, timestamp INTEGER NOT NULL, firebaseId TEXT, isSynced INTEGER NOT NULL)")
            db.execSQL("INSERT INTO transaction_table VALUES (73,'old',125.50,'EXPENSE','Receipt',100,'FOOD',NULL,NULL,NULL,NULL,NULL,0)")
            db.version = 13
        }
        val database = Room.databaseBuilder(context, FinanceDatabase::class.java, name)
            .addMigrations(AccountMigration.MIGRATION_13_14).build()
        try {
            database.openHelper.writableDatabase.query("SELECT amount FROM legacy_v13_transaction_table WHERE id = 73").use {
                assertTrue(it.moveToFirst()); assertEquals(125.50, it.getDouble(0), 0.0)
            }
            assertTrue(database.transactionDao().getAllTransactionsOneShot().isEmpty())
            database.openHelper.writableDatabase.query("SELECT ownerId FROM transaction_table WHERE id = 73").use {
                assertTrue(it.moveToFirst()); assertEquals("", it.getString(0))
            }
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
