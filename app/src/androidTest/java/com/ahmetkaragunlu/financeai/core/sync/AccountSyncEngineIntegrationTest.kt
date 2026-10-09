package com.ahmetkaragunlu.financeai.core.sync

import android.database.sqlite.SQLiteException
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.testing.EmulatorAccountFixture
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.transaction.data.repository.TransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import dagger.Lazy
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

    private suspend fun conflict(f: EmulatorAccountFixture, owner: String, record: String) = run {
        f.activate(owner)
        val repository = repository(f)
        repository.insertTransaction(draft(record))
        val engine = f.engine(setOf(store(f)))
        engine.synchronize(f.local.session.requireAccount())
        val row = repository.observeTransactions().first().single()
        repository.updateDetails(row, 30.75, "local", CategoryType.FOOD)
        f.firestore.collection("transactions").document(record)
            .update(mapOf("amountMinor" to 4000L, "amount" to 40.0, "note" to "remote", "revision" to 2L)).await()
        engine.synchronize(f.local.session.requireAccount())
        checkNotNull(f.local.database.syncRecordDao().get(owner, "transactions", record))
    }

    @Test fun explicitConflictChoicesKeepOneFinancialIdentityAndTheChosenDurableIntent(): Unit = runBlocking {
        for (keepLocal in listOf(true, false)) {
            val f = EmulatorAccountFixture()
            try { withTimeout(25_000) {
                val owner = "resolve-${UUID.randomUUID()}"; val record = "receipt-${UUID.randomUUID()}"
                val conflict = conflict(f, owner, record)
                val id = repository(f).observeTransactions().first().single().id
                assertEquals(2L, conflict.conflictRevision)
                val engine = f.engine(setOf(store(f)))
                engine.resolve(conflict, keepLocal)
                val after = checkNotNull(f.local.database.syncRecordDao().get(owner, "transactions", record))
                assertNull(after.conflictRevision)
                assertEquals(id, repository(f).observeTransactions().first().single().id)
                if (keepLocal) {
                    assertEquals(conflict.mutationId, after.mutationId)
                    assertEquals(3075L, SyncPayload.decode(checkNotNull(after.pendingPayload))["amountMinor"])
                    engine.synchronize(f.local.session.requireAccount())
                    assertEquals(3075L, f.firestore.collection("transactions").document(record).get().await().getLong("amountMinor"))
                } else {
                    assertNull(after.mutationId)
                    assertEquals(40.0, repository(f).observeTransactions().first().single().amount, 0.0)
                }
                assertEquals(1, repository(f).observeTransactions().first().size)
            } } finally { f.close() }
        }
    }

    @Test fun changedRemoteConflictRequiresAnotherChoiceInsteadOfApplyingAnOldPrompt(): Unit = runBlocking {
        val f = EmulatorAccountFixture()
        try { withTimeout(25_000) {
            val owner = "resolve-${UUID.randomUUID()}"; val record = "receipt-${UUID.randomUUID()}"
            val old = conflict(f, owner, record)
            f.firestore.collection("transactions").document(record).update(mapOf("amountMinor" to 4500L, "amount" to 45.0, "revision" to 3L)).await()
            f.engine(setOf(store(f))).resolve(old, false)
            val updated = checkNotNull(f.local.database.syncRecordDao().get(owner, "transactions", record))
            assertEquals(old.mutationId, updated.mutationId)
            assertEquals(3L, updated.conflictRevision)
            assertEquals(4500L, SyncPayload.decode(checkNotNull(updated.conflictPayload))["amountMinor"])
            assertEquals(30.75, repository(f).observeTransactions().first().single().amount, 0.0)
        } } finally { f.close() }
    }

    @Test fun newLocalEditDuringResolutionPreparationCannotBeErasedByTheOldChoice(): Unit = runBlocking {
        val f = EmulatorAccountFixture()
        val release = CompletableDeferred<Unit>()
        try { withTimeout(25_000) {
            val owner = "resolve-${UUID.randomUUID()}"; val record = "receipt-${UUID.randomUUID()}"
            val old = conflict(f, owner, record)
            val actual = store(f); val entered = CompletableDeferred<Unit>()
            val controlled = object : RemoteRecordStore by actual {
                override suspend fun prepare(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Map<String, Any?> {
                    entered.complete(Unit); release.await(); return actual.prepare(account, remoteId, data)
                }
            }
            val resolving = async { f.engine(setOf(controlled)).resolve(old, false) }
            entered.await()
            val repository = repository(f)
            repository.updateDetails(repository.observeTransactions().first().single(), 35.25, "newer", CategoryType.GROCERIES)
            val newer = f.local.database.syncRecordDao().get(owner, "transactions", record)
            release.complete(Unit); resolving.await()
            assertEquals(newer, f.local.database.syncRecordDao().get(owner, "transactions", record))
            assertEquals(35.25, repository.observeTransactions().first().single().amount, 0.0)
        } } finally { release.complete(Unit); f.close() }
    }

    @Test fun foreignOwnershipAndLocalTransactionFailureCannotPartiallyResolveAConflict(): Unit = runBlocking {
        val f = EmulatorAccountFixture()
        try { withTimeout(25_000) {
            val owner = "resolve-${UUID.randomUUID()}"; val record = "receipt-${UUID.randomUUID()}"
            val old = conflict(f, owner, record)
            val before = repository(f).observeTransactions().first().single()
            val engine = f.engine(setOf(store(f)))
            val ref = f.firestore.collection("transactions").document(record)
            ref.update("userId", "foreign-synthetic-owner").await()
            assertThrows(DataAccessException.AccessDenied::class.java) { runBlocking { engine.resolve(old, false) } }
            assertEquals(old, f.local.database.syncRecordDao().get(owner, "transactions", record))
            ref.update("userId", owner).await()
            f.local.rejectSyncWrites()
            assertThrows(SQLiteException::class.java) { runBlocking { engine.resolve(old, false) } }
            assertEquals(before, repository(f).observeTransactions().first().single())
            assertEquals(old, f.local.database.syncRecordDao().get(owner, "transactions", record))
        } } finally { f.close() }
    }

    @Test fun staleRevisionAndInvalidCurrencyCannotOverwriteAcceptedLocalData(): Unit = runBlocking {
        val f = EmulatorAccountFixture()
        try { withTimeout(25_000) {
            val owner = "receive-${UUID.randomUUID()}"; val record = "receipt-${UUID.randomUUID()}"
            f.activate(owner); repository(f).insertTransaction(draft(record))
            val engine = f.engine(setOf(store(f))); val account = f.local.session.requireAccount()
            engine.synchronize(account)
            val before = repository(f).observeTransactions().first().single()
            val ref = f.firestore.collection("transactions").document(record)
            ref.update(mapOf("note" to "old revision", "revision" to 0L)).await()
            engine.synchronize(account)
            assertEquals(before, repository(f).observeTransactions().first().single())
            ref.update(mapOf("currencyCode" to "EUR", "revision" to 2L)).await()
            engine.synchronize(account)
            assertEquals(before, repository(f).observeTransactions().first().single())
            assertFalse(f.local.database.syncRecordDao().get(owner, "transactions", record)?.permanentFailure == true)
        } } finally { f.close() }
    }
}
