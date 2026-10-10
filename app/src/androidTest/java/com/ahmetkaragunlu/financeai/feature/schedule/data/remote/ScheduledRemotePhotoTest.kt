package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderPresenter
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

class ScheduledRemotePhotoTest {
    @Test fun sharedPhotoRulesKeepPlanIdentityFlagsAndFeatureOwnedCancellation(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val file = File.createTempFile("scheduled-photo-test-", ".jpg", f.context.cacheDir)
            try {
                val id = f.database.scheduledTransactionDao().insertScheduledTransaction(ScheduledTransactionEntity(ownerId = "A",
                    firestoreId = "plan", currencyCode = "USD", amountMinor = 1000, type = TransactionType.EXPENSE,
                    category = CategoryType.FOOD, note = null, scheduledDate = 100, photoUri = file.path,
                    notificationSent = true, expirationNotificationSent = true))
                val metadata = mapOf("photoStorageUrl" to "https://example.test/plan", "photoVersion" to "v1")
                f.database.syncRecordDao().save(SyncRecord("A", "scheduled_transactions", "plan", basePayload = SyncPayload.encode(metadata)))
                val reminders = spy(ReminderScheduler(f.workManager, f.clock))
                val presenter = mock(ReminderPresenter::class.java)
                val cache = PhotoRemoteCache(f.context,
                    { error("No media download expected") }, f.session, Dispatchers.IO)
                val store = ScheduledTransactionRemoteStore(f.database, cache, reminders, presenter)
                val account = f.session.requireAccount()
                val normalized = store.normalize(metadata + mapOf("amountMinor" to 2550L, "currencyCode" to "USD",
                    "type" to "EXPENSE", "category" to "FOOD", "scheduledDate" to 200L), account)
                val prepared = store.prepare(account, "plan", normalized)
                store.apply(account, "plan", prepared)
                val row = checkNotNull(f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan"))
                assertEquals(id, row.id)
                assertEquals(file.path, row.photoUri)
                assertTrue(row.notificationSent)
                assertTrue(row.expirationNotificationSent)
                verify(reminders).wake("A", "plan")
                val removed = prepared + mapOf("photoRemoved" to true)
                assertSame(removed, store.prepare(account, "plan", removed))
                store.apply(account, "plan", removed)
                assertNull(f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan")?.photoUri)
                store.apply(account, "plan", null)
                verify(reminders).cancel("A", "plan", id)
                verify(presenter).cancel("A", "plan")
            } finally { file.delete() }
        }
    }
}
