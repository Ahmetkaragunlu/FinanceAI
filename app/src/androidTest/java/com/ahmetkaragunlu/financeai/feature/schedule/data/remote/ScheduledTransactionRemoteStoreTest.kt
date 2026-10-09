package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderPresenter
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

class ScheduledTransactionRemoteStoreTest {
    private val legacy = mapOf<String, Any?>("amount" to 12.50, "type" to "EXPENSE", "category" to "FOOD",
        "scheduledDate" to 123L, "notificationSent" to true, "expirationNotificationSent" to true,
        "locationShort" to "short", "latitude" to 41.0)

    @Test fun normalizationAndApplyPreservePlanIdentityLocalDisplayFlagsAndDueDispatch() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val reminders = spy(ReminderScheduler(f.workManager, f.clock))
            val presenter = mock(ReminderPresenter::class.java)
            val store = ScheduledTransactionRemoteStore(f.database,
                PhotoRemoteCache(f.context, Lazy { error("No media network expected") }, f.session, Dispatchers.IO), reminders, presenter)
            val account = f.session.requireAccount()
            val first = store.normalize(legacy, account)
            assertEquals(1250L, first["amountMinor"])
            assertEquals(true, first["notificationSent"])
            store.apply(account, "plan", first)
            val original = checkNotNull(f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan"))
            assertFalse(original.notificationSent || original.expirationNotificationSent)
            assertNull(original.note)
            assertEquals("short", original.locationShort)
            verify(reminders).wake("A", "plan", f.clock.millis())
            clearInvocations(reminders)
            store.apply(account, "plan", store.normalize(legacy + ("amountMinor" to 5000L), account))
            verify(reminders, never()).wake("A", "plan", f.clock.millis())
            store.apply(account, "plan", store.normalize(legacy + mapOf("scheduledDate" to 456L, "note" to "changed"), account))
            val updated = checkNotNull(f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan"))
            assertEquals(original.id, updated.id)
            assertEquals(456L, updated.scheduledDate)
            assertEquals("changed", updated.note)
            verify(reminders).wake("A", "plan", f.clock.millis())
            store.apply(account, "plan", null)
            assertNull(f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan"))
            verify(reminders).cancel("A", "plan", original.id)
            verify(presenter).cancel("A", "plan")
        }
    }

    @Test fun invalidRemotePlanCannotOverwriteRetainedLocalData() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val store = ScheduledTransactionRemoteStore(f.database,
                PhotoRemoteCache(f.context, Lazy { error("No media network expected") }, f.session, Dispatchers.IO),
                ReminderScheduler(f.workManager, f.clock), mock(ReminderPresenter::class.java))
            val account = f.session.requireAccount()
            store.apply(account, "plan", store.normalize(legacy, account))
            val original = f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan")
            for (patch in listOf(mapOf("currencyCode" to "EUR"), mapOf("type" to "unknown"),
                mapOf("category" to "unknown"), mapOf("amountMinor" to 1.5))) {
                try { store.apply(account, "plan", legacy + patch); fail("Expected invalid remote plan") }
                catch (e: Exception) { assertTrue(e is DataAccessException.InvalidRemoteData || e is IllegalArgumentException || e is ArithmeticException) }
                assertEquals(original, f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan"))
            }
        }
    }
}
