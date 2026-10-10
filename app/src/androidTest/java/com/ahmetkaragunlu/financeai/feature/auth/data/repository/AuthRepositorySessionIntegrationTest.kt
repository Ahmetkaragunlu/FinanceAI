package com.ahmetkaragunlu.financeai.feature.auth.data.repository

import android.app.NotificationManager
import android.content.ContextWrapper
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.session.SessionWorkRestorer
import com.ahmetkaragunlu.financeai.core.sync.testing.EmulatorAccountFixture
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.feature.auth.data.remote.AuthLookupRemote
import com.ahmetkaragunlu.financeai.feature.transaction.data.repository.TransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

class AuthRepositorySessionIntegrationTest {
    @Test
    fun failedLocalCleanupRestoresTheActualSignedInAccountAndRetryRetainsItsPreferences() = runBlocking {
        val f = EmulatorAccountFixture()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val owner = "auth-logout-${UUID.randomUUID()}"
            f.activate(owner)
            val original = f.local.session.requireAccount()
            val originalPreferences = f.local.database.accountDao().get(owner)
            val financialId = "retained-${UUID.randomUUID()}"
            TransactionRepositoryImpl(f.local.database.transactionDao(), f.local.database,
                f.local.session, f.local.pending, f.local.scheduler).insertTransaction(
                Transaction(firestoreId = financialId, amount = 12.50, date = 100,
                    transaction = TransactionType.EXPENSE, category = CategoryType.FOOD))
            val pendingIntent = f.local.database.syncRecordDao().get(owner, "transactions", financialId)
            val notifications = mock(NotificationManager::class.java)
            val expected = IllegalStateException("synthetic local cleanup failure")
            var first = true
            doAnswer {
                if (first) { first = false; throw expected }
                null
            }.`when`(notifications).cancelAll()
            val context = object : ContextWrapper(f.local.context) {
                override fun getSystemService(name: String): Any? =
                    if (name == NOTIFICATION_SERVICE) notifications else super.getSystemService(name)
            }
            val coordinator = SessionCoordinator(f.auth, f.firestore, f.local.database, f.local.session,
                f.local.scheduler, { f.engine(emptySet()) }, f.local.workManager,
                object : SessionWorkRestorer { override suspend fun restore(account: ActiveAccount) {} },
                context, scope)
            val tokens = mock(FCMTokenManager::class.java)
            doAnswer { }.`when`(tokens).removeFCMToken()
            val repository = AuthRepositoryImpl(f.auth, f.firestore, coordinator, tokens,
                mock(AuthLookupRemote::class.java)
            ) {}
            try { repository.signOut(); fail("Expected failed cleanup") }
            catch (e: IllegalStateException) { assertSame(expected, e) }
            assertEquals(owner, f.auth.currentUser?.uid)
            assertEquals(owner, f.local.session.account.value?.ownerId)
            assertNotEquals(original.generation, f.local.session.requireAccount().generation)
            assertEquals(originalPreferences, f.local.database.accountDao().get(owner))
            assertEquals(pendingIntent, f.local.database.syncRecordDao().get(owner, "transactions", financialId))
            repository.signOut()
            assertNull(f.auth.currentUser)
            assertNull(f.local.session.account.value)
            assertEquals(originalPreferences, f.local.database.accountDao().get(owner))
            assertEquals(pendingIntent, f.local.database.syncRecordDao().get(owner, "transactions", financialId))
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            f.close()
        }
    }
}
