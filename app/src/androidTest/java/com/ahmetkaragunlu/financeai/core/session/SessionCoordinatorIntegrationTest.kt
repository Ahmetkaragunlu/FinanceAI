package com.ahmetkaragunlu.financeai.core.session

import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.sync.testing.EmulatorAccountFixture
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.transaction.data.repository.TransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import dagger.Lazy
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test

class SessionCoordinatorIntegrationTest {
    private fun coordinator(f: EmulatorAccountFixture, scope: CoroutineScope, restored: MutableList<String>): SessionCoordinator {
        val photos = PhotoRemoteCache(f.local.context, Lazy { error("No media network expected") }, f.local.session, Dispatchers.IO)
        val engine = f.engine(setOf(TransactionRemoteStore(f.local.database, photos)))
        val restorer = object : SessionWorkRestorer {
            override suspend fun restore(account: ActiveAccount) { restored += account.ownerId }
        }
        return SessionCoordinator(f.auth, f.firestore, f.local.database, f.local.session,
            f.local.scheduler, Lazy { engine }, f.local.workManager, restorer, f.local.context, scope)
    }

    @Test fun onlyVerifiedAccountsBecomeReadyAndRemotePreferencesAreStoredBeforeWorkRestoration() = runBlocking {
        val f = EmulatorAccountFixture()
        val owner = "session-${UUID.randomUUID()}"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val restored = CopyOnWriteArrayList<String>()
        try {
            withTimeout(25_000) {
                f.firestore.collection("users").document(owner).set(mapOf("currencyCode" to "USD", "timeZoneId" to "Europe/Istanbul")).await()
                val coordinator = coordinator(f, scope, restored)
                f.signIn(owner, verified = false)
                coordinator.prepare()
                assertNull(f.local.session.account.value)
                assertTrue(restored.isEmpty())
                coordinator.start()
                f.signIn(owner)
                val account = f.local.session.account.first { it?.ownerId == owner }
                assertEquals("USD", account?.currencyCode)
                assertEquals("Europe/Istanbul", account?.timeZoneId)
                assertEquals("Europe/Istanbul", f.local.database.accountDao().get(owner)?.timeZoneId)
                while (!restored.contains(owner)) delay(10)
            }
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            f.close()
        }
    }

    @Test fun switchingAccountsAndSigningOutKeepPendingFinancialIntentWithoutExposingItToTheNextAccount() = runBlocking {
        val f = EmulatorAccountFixture()
        val first = "session-A-${UUID.randomUUID()}"
        val second = "session-B-${UUID.randomUUID()}"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            withTimeout(25_000) {
                f.firestore.collection("users").document(first).set(mapOf("currencyCode" to "USD", "timeZoneId" to "UTC")).await()
                f.firestore.collection("users").document(second).set(mapOf("currencyCode" to "EUR", "timeZoneId" to "UTC")).await()
                val coordinator = coordinator(f, scope, CopyOnWriteArrayList())
                coordinator.start()
                f.signIn(first)
                val original = checkNotNull(f.local.session.account.first { it?.ownerId == first })
                val repository = TransactionRepositoryImpl(f.local.database.transactionDao(), f.local.database,
                    f.local.session, f.local.pending, f.local.scheduler)
                val record = "receipt-${UUID.randomUUID()}"
                repository.insertTransaction(Transaction(firestoreId = record, amount = 25.50, date = 100,
                    transaction = TransactionType.EXPENSE, category = CategoryType.FOOD))
                val pending = checkNotNull(f.local.database.syncRecordDao().get(first, "transactions", record))
                f.signIn(second)
                f.local.session.account.first { it?.ownerId == second }
                assertFalse(f.local.session.isCurrent(original))
                assertTrue(repository.observeTransactions().first().isEmpty())
                assertEquals(pending, f.local.database.syncRecordDao().get(first, "transactions", record))
                coordinator.signOut()
                assertNull(f.local.session.account.value)
                assertEquals(pending, f.local.database.syncRecordDao().get(first, "transactions", record))
                f.signIn(first)
                val returned = checkNotNull(f.local.session.account.first { it?.ownerId == first })
                assertNotEquals(original.generation, returned.generation)
                assertEquals(25.50, repository.observeTransactions().first().single().amount, 0.0)
                assertEquals(pending.mutationId, f.local.database.syncRecordDao().get(first, "transactions", record)?.mutationId)
            }
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            f.close()
        }
    }
}
