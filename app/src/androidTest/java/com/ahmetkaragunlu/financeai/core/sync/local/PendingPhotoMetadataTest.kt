package com.ahmetkaragunlu.financeai.core.sync.local

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PendingPhotoMetadataTest {
    @Test fun pendingMetadataWinsOverBaselineWhileNewValuesWinOverPreservedMetadata(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val baseline = mapOf("photoStorageUrl" to "https://base", "photoVersion" to "base", "photoIntent" to "base",
                "photoRemoved" to false, "localPhotoUri" to "/private-base.jpg")
            val pending = baseline + mapOf("photoStorageUrl" to "https://pending", "photoVersion" to "pending")
            f.database.syncRecordDao().save(SyncRecord("A", "transactions", "receipt",
                basePayload = SyncPayload.encode(baseline), pendingPayload = SyncPayload.encode(pending)))
            f.pending.record("A", "transactions", "receipt", mapOf("note" to "edited", "photoIntent" to "new-intent"))
            val payload = SyncPayload.decode(checkNotNull(f.database.syncRecordDao().get("A", "transactions", "receipt")?.pendingPayload))
            assertEquals("https://pending", payload["photoStorageUrl"])
            assertEquals("pending", payload["photoVersion"])
            assertEquals("new-intent", payload["photoIntent"])
            assertEquals("edited", payload["note"])
            assertFalse(payload.containsKey("localPhotoUri"))
            f.pending.record("A", "transactions", "receipt", mapOf("photoRemoved" to true, "photoStorageUrl" to null))
            val removed = SyncPayload.decode(checkNotNull(f.database.syncRecordDao().get("A", "transactions", "receipt")?.pendingPayload))
            assertEquals(true, removed["photoRemoved"])
            assertEquals(null, removed["photoStorageUrl"])
        }
    }
}
