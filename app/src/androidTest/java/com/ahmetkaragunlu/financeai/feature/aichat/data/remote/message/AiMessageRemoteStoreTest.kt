package com.ahmetkaragunlu.financeai.feature.aichat.data.remote.message

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMessageRemoteStoreTest {
    @Test fun normalizedChatMessagesKeepLocalIdentityTimestampAndSeparateAccountHistories() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val store = AiMessageRemoteStore(f.database)
            val first = store.normalize(mapOf("text" to "original", "isAi" to false, "timestamp" to 300L,
                "diagnostic" to "must not persist"), f.session.requireAccount())
            assertEquals(mapOf("text" to "original", "isAi" to false, "timestamp" to 300L), first)
            store.apply(f.session.requireAccount(), "message", first)
            val original = checkNotNull(f.database.aiMessageDao().getMessageByFirebaseId("message"))
            store.apply(f.session.requireAccount(), "message", store.normalize(mapOf("text" to "answer", "isAi" to true,
                "timestamp" to 400L), f.session.requireAccount()))
            val updated = checkNotNull(f.database.aiMessageDao().getMessageByFirebaseId("message"))
            assertEquals(original.id, updated.id)
            assertEquals("answer", updated.text)
            assertEquals(400L, updated.timestamp.time)
            assertTrue(updated.isAi && updated.isSynced)
            assertEquals(mapOf("text" to "", "isAi" to false, "timestamp" to 0L),
                store.normalize(mapOf("text" to 1, "isAi" to "invalid", "timestamp" to "invalid"), f.session.requireAccount()))
            f.activate("B", "EUR")
            store.apply(f.session.requireAccount(), "message", first)
            store.apply(f.session.requireAccount(), "message", null)
            assertNull(f.database.aiMessageDao().getMessageByFirebaseId("message"))
            f.activate()
            assertEquals(updated, f.database.aiMessageDao().getMessageByFirebaseId("message"))
            store.apply(f.session.requireAccount(), "message", null)
            store.apply(f.session.requireAccount(), "message", null)
            assertNull(f.database.aiMessageDao().getMessageByFirebaseId("message"))
        }
    }
}
