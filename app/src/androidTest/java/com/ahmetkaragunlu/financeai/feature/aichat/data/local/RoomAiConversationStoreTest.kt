package com.ahmetkaragunlu.financeai.feature.aichat.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow
import com.ahmetkaragunlu.financeai.core.sync.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RoomAiConversationStoreTest {
    @Test fun retryPreservesLocalIdentityCreatedAtAndPendingMutationAndRejectsOtherAccount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val session = AccountSession().apply { activate("A", "USD") }
            val account = session.requireAccount()
            database.accountDao().setActive(ActiveAccountRow(ownerId = "A"))
            val pending = PendingChanges(database)
            val clock = Clock.fixed(Instant.ofEpochMilli(12345), ZoneOffset.UTC)
            val store = RoomAiConversationStore(database, session, pending, SyncScheduler(WorkManager.getInstance(context)), clock)
            store.save(account, "request", "question", false)
            val row = database.aiMessageDao().getMessageByFirebaseId("request")!!
            val mutation = database.syncRecordDao().get("A", "ai_messages", "request")!!.mutationId
            store.save(account, "request", "question", false)
            assertEquals(row, database.aiMessageDao().getMessageByFirebaseId("request"))
            assertEquals(12345L, row.timestamp.time)
            assertEquals(mutation, database.syncRecordDao().get("A", "ai_messages", "request")!!.mutationId)
            assertEquals(1, database.syncRecordDao().pending("A").size)
            session.activate("B", "USD")
            database.accountDao().setActive(ActiveAccountRow(ownerId = "B"))
            try { store.save(account, "request_reply", "late", true); fail("Foreign result accepted") }
            catch (_: CancellationException) { }
            assertFalse(store.contains(session.requireAccount(), "request"))
            database.accountDao().setActive(ActiveAccountRow(ownerId = "A"))
            assertNull(database.aiMessageDao().getMessageByFirebaseId("request_reply"))
        } finally { database.close(); WorkManagerTestInitHelper.closeWorkDatabase() }
    }
}
