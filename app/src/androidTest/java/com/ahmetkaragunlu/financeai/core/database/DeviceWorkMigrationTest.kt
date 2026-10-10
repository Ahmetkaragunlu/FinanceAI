package com.ahmetkaragunlu.financeai.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceWorkMigrationTest {
    // Seed the exact exported schema, then let the real generated Room implementation validate
    // migration. Avoid changing production serialization just for the optional test-helper ABI.
    private fun schema(version: Int): JsonObject =
        InstrumentationRegistry.getInstrumentation().context.assets
            .open("${FinanceDatabase::class.java.name}/$version.json")
            .bufferedReader(Charsets.UTF_8)
            .use { JsonParser.parseReader(it).asJsonObject.getAsJsonObject("database") }

    @Test
    fun exportedVersion14MigratesTo15WithoutLosingMoneyPhotosAccountOwnershipOrPendingIntent() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "migration14-${UUID.randomUUID()}"
            try {
                val file = context.getDatabasePath(name)
                check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
                SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                    val exported = schema(14)
                    for (entry in exported.getAsJsonArray("entities")) {
                        val entity = entry.asJsonObject
                        val table = entity["tableName"].asString
                        db.execSQL(
                            entity["createSql"].asString.replace(
                                "${'$'}{TABLE_NAME}",
                                table
                            )
                        )
                        entity.getAsJsonArray("indices")?.forEach { index ->
                            db.execSQL(
                                index.asJsonObject["createSql"].asString.replace(
                                    "${'$'}{TABLE_NAME}",
                                    table
                                )
                            )
                        }
                    }
                    for (query in exported.getAsJsonArray("setupQueries")) db.execSQL(query.asString)
                    db.execSQL("INSERT INTO account_preferences(ownerId,currencyCode) VALUES ('A','USD'),('B','EUR')")
                    db.execSQL("INSERT INTO active_account(id,ownerId) VALUES (0,'A')")
                    db.execSQL("INSERT INTO transaction_table(id,firestoreId,amountMinor,`transaction`,note,date,category,photoUri,ownerId,currencyCode,syncedToFirebase) VALUES (73,'receipt',12550,'EXPENSE','original',100,'FOOD','/synthetic/receipt.jpg','A','USD',0),(74,'receipt',999,'EXPENSE','foreign',101,'FOOD',NULL,'B','EUR',0)")
                    db.execSQL("INSERT INTO budget_table(id,firestoreId,budgetType,category,amountMinor,limitPercentage,ownerId,currencyCode,syncedToFirebase) VALUES (81,'budget','CATEGORY_PERCENTAGE','FOOD',20000,12.5,'A','USD',0)")
                    db.execSQL("INSERT INTO scheduled_transactions_table(id,firestoreId,amountMinor,type,category,note,scheduledDate,expirationNotificationSent,notificationSent,photoUri,ownerId,currencyCode,syncedToFirebase) VALUES (91,'plan',5050,'EXPENSE','FOOD',NULL,200,1,1,'/synthetic/plan.jpg','A','USD',0)")
                    db.execSQL("INSERT INTO ai_messages(id,text,isAi,timestamp,firebaseId,ownerId,isSynced) VALUES (101,'pending chat',0,300,'chat','A',0),(102,'foreign chat',1,301,'chat','B',1)")
                    db.execSQL("INSERT INTO sync_records(ownerId,`collection`,remoteId,basePayload,baseRevision,pendingPayload,pendingDelete,mutationId,conflictPayload,conflictRevision,permanentFailure) VALUES ('A','transactions','receipt','{\"amountMinor\":12550}',4000000001,'{\"amountMinor\":15000}',0,'pending-mutation',NULL,NULL,0)")
                    db.version = 14
                }
                val db = Room.databaseBuilder(context, FinanceDatabase::class.java, name)
                    .addMigrations(DeviceWorkMigration.MIGRATION_14_15).build()
                try {
                    val migrated =
                        db.openHelper.writableDatabase // Runs migration and actual Room schema validation.
                    assertEquals(15, migrated.version)
                    val expectedTables = schema(15).getAsJsonArray("entities")
                        .map { it.asJsonObject["tableName"].asString }.toSet()
                    val actualTables = mutableSetOf<String>()
                    migrated.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table','android_metadata')")
                        .use { cursor ->
                            while (cursor.moveToNext()) actualTables += cursor.getString(0)
                        }
                    assertEquals(expectedTables, actualTables)
                    for (table in listOf(
                        "photo_operations",
                        "schedule_commands",
                        "reminder_state",
                        "push_events",
                        "token_operations"
                    )) {
                        migrated.query("SELECT COUNT(*) FROM $table").use { cursor ->
                            assertTrue(cursor.moveToFirst()); assertEquals(
                            0,
                            cursor.getInt(0)
                        )
                        }
                    }
                    val receipt = db.transactionDao().getAllTransactionsOneShot().single()
                    assertEquals(73, receipt.id)
                    assertEquals(12550L, receipt.amountMinor)
                    assertEquals("USD", receipt.currencyCode)
                    assertEquals("/synthetic/receipt.jpg", receipt.photoUri)
                    val budget = db.budgetDao().getAllBudgetsOneShot().single()
                    assertEquals(81, budget.id)
                    assertEquals(20000L, budget.amountMinor)
                    assertEquals(12.5, checkNotNull(budget.limitPercentage), 0.0)
                    val plan =
                        db.scheduledTransactionDao().getScheduledTransactionsOneShot().single()
                    assertEquals(91L, plan.id)
                    assertEquals(5050L, plan.amountMinor)
                    assertNull(plan.note)
                    assertTrue(plan.notificationSent && plan.expirationNotificationSent)
                    assertEquals("/synthetic/plan.jpg", plan.photoUri)
                    val chat = db.aiMessageDao().observeMessages().first().single()
                    assertEquals(101L, chat.id)
                    assertEquals("pending chat", chat.text)
                    assertEquals(300L, chat.timestamp.time)
                    assertEquals(false, chat.isSynced)
                    val pending =
                        checkNotNull(db.syncRecordDao().get("A", "transactions", "receipt"))
                    assertEquals(4000000001L, pending.baseRevision)
                    assertEquals("pending-mutation", pending.mutationId)
                    assertEquals("{\"amountMinor\":15000}", pending.pendingPayload)
                    assertNull(checkNotNull(db.accountDao().get("A")).timeZoneId)
                    db.accountDao().setActive(ActiveAccountRow(ownerId = "B"))
                    assertEquals(74, db.transactionDao().getAllTransactionsOneShot().single().id)
                    assertEquals(
                        "foreign chat",
                        db.aiMessageDao().observeMessages().first().single().text
                    )
                    assertEquals(pending, db.syncRecordDao().get("A", "transactions", "receipt"))
                } finally {
                    db.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }
}
