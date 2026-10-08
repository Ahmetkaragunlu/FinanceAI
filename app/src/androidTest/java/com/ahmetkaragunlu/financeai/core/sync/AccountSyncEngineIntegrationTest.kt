package com.ahmetkaragunlu.financeai.core.sync

import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.testing.EmulatorAccountFixture
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.transaction.data.repository.TransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import dagger.Lazy
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test

class AccountSyncEngineIntegrationTest {
    private fun store(f: EmulatorAccountFixture) = TransactionRemoteStore(f.local.database,
        PhotoRemoteCache(f.local.context, Lazy { error("No remote photo operation expected") }, f.local.session, Dispatchers.IO))
    private fun repository(f: EmulatorAccountFixture) = TransactionRepositoryImpl(
        f.local.database.transactionDao(), f.local.database, f.local.session, f.local.pending, f.local.scheduler
    )
    private fun draft(id: String) = Transaction(firestoreId = id, amount = 25.50, date = 100,
        transaction = TransactionType.EXPENSE, category = CategoryType.FOOD, note = "before")

    @Test fun anOldRemoteAcknowledgementCannotEraseANewerDurableLocalEdit() = runBlocking {
        val f = EmulatorAccountFixture()
        val owner = "engine-${UUID.randomUUID()}"
        val record = "receipt-${UUID.randomUUID()}"
        val release = CompletableDeferred<Unit>()
        try {
            withTimeout(25_000) {
                f.activate(owner)
                val repository = repository(f)
                repository.insertTransaction(draft(record))
                val initial = repository.observeTransactions().first().single()
                val entered = CompletableDeferred<Unit>()
                val actual = store(f)
                var first = true
                val controlled = object : RemoteRecordStore by actual {
                    override suspend fun prepare(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Map<String, Any?> {
                        if (first) { first = false; entered.complete(Unit); release.await() }
                        return actual.prepare(account, remoteId, data)
                    }
                }
                val engine = f.engine(setOf(controlled))
                val account = f.local.session.requireAccount()
                val syncing = async { engine.synchronize(account) }
                entered.await()
                repository.updateDetails(initial, 30.75, "new local note", CategoryType.GROCERIES)
                val newer = checkNotNull(f.local.database.syncRecordDao().get(owner, "transactions", record))
                release.complete(Unit)
                syncing.await()
                val retained = checkNotNull(f.local.database.syncRecordDao().get(owner, "transactions", record))
                assertEquals(newer.mutationId, retained.mutationId)
                assertEquals(3075L, SyncPayload.decode(checkNotNull(retained.pendingPayload))["amountMinor"])
                assertEquals("new local note", repository.observeTransactions().first().single().note)
                engine.synchronize(account)
                assertNull(checkNotNull(f.local.database.syncRecordDao().get(owner, "transactions", record)).mutationId)
                assertEquals(3075L, f.firestore.collection("transactions").document(record).get().await().getLong("amountMinor"))
            }
        } finally { release.complete(Unit); f.close() }
    }

    @Test fun anAccountSwitchDuringRemotePreparationRejectsTheOldResultAndKeepsItsPendingIntent() = runBlocking {
        val f = EmulatorAccountFixture()
        val firstOwner = "engine-A-${UUID.randomUUID()}"
        val secondOwner = "engine-B-${UUID.randomUUID()}"
        val record = "receipt-${UUID.randomUUID()}"
        val release = CompletableDeferred<Unit>()
        try {
            withTimeout(25_000) {
                f.activate(firstOwner)
                val repository = repository(f)
                repository.insertTransaction(draft(record))
                val entered = CompletableDeferred<Unit>()
                val actual = store(f)
                val controlled = object : RemoteRecordStore by actual {
                    override suspend fun prepare(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Map<String, Any?> {
                        entered.complete(Unit)
                        release.await()
                        return actual.prepare(account, remoteId, data)
                    }
                }
                val syncing = async { f.engine(setOf(controlled)).synchronize(f.local.session.requireAccount()) }
                entered.await()
                f.activate(secondOwner, "EUR")
                release.complete(Unit)
                try { syncing.await(); fail("Stale network result accepted") } catch (_: CancellationException) { }
                assertTrue(repository.observeTransactions().first().isEmpty())
                assertNotNull(checkNotNull(f.local.database.syncRecordDao().get(firstOwner, "transactions", record)).mutationId)
                assertTrue(f.local.database.syncRecordDao().forAccount(secondOwner).isEmpty())
            }
        } finally { release.complete(Unit); f.close() }
    }
}
