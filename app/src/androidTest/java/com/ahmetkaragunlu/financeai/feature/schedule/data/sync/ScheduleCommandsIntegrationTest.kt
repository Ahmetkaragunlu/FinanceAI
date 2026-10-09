package com.ahmetkaragunlu.financeai.feature.schedule.data.sync

import android.database.sqlite.SQLiteException
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.core.sync.testing.EmulatorAccountFixture
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduleCommand
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.ScheduledTransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.schedule.domain.error.ScheduleException
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderPresenter
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import dagger.Lazy
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock

class ScheduleCommandsIntegrationTest {
    private class Fixture {
        val sdk = EmulatorAccountFixture()
        val owner = "command-${UUID.randomUUID()}"
        val plan = "plan-${UUID.randomUUID()}"
        val operation = "operation-${UUID.randomUUID()}"
        val financial = "completed_$plan"
        val account get() = sdk.local.session.requireAccount()
        private val cache = PhotoRemoteCache(sdk.local.context, Lazy { error("No media network expected") }, sdk.local.session, Dispatchers.IO)
        val transactions = TransactionRemoteStore(sdk.local.database, cache)
        val schedules = ScheduledTransactionRemoteStore(sdk.local.database, cache,
            ReminderScheduler(sdk.local.workManager, sdk.local.clock), mock(ReminderPresenter::class.java))
        val commands = ScheduleCommands(sdk.local.database, sdk.firestore, sdk.auth, sdk.local.session, schedules,
            transactions, sdk.local.clock, CompletedPlanEditResolution(sdk.local.database, sdk.local.pending, transactions,
                schedules, PhotoWorkScheduler(sdk.local.workManager, sdk.local.session, sdk.local.database)))
        fun raw(financial: Boolean = false, amount: Long = 1000) = mapOf<String, Any?>("userId" to owner,
            "currencyCode" to "USD", "amountMinor" to amount, "category" to "FOOD", "note" to "remote",
            (if (financial) "transaction" else "type") to "EXPENSE", (if (financial) "date" else "scheduledDate") to 100L,
            "revision" to 8L, "deleted" to false)
        suspend fun start(type: String = "complete", sent: String? = null) {
            sdk.activate(owner)
            sdk.local.database.scheduleCommandDao().insert(ScheduleCommand(operation, owner, plan, 100, type, 100, financialPayload = sent))
        }
        suspend fun receipt(outcome: String?, target: String? = null) {
            val data = mutableMapOf<String, Any?>("userId" to owner, "processed" to (outcome != null))
            if (outcome != null) data["outcome"] = outcome
            if (target != null) data["conflictTarget"] = target
            sdk.firestore.collection("schedule_commands").document(operation).set(data).await()
        }
        suspend fun close() = sdk.close()
    }

    @Test fun unprocessedReceiptsRetainCommandsAndAcceptedDuplicateReceiptsAreIdempotent(): Unit = runBlocking {
        val f = Fixture()
        try { withTimeout(25_000) {
            f.start("snooze")
            assertThrows(ScheduleException.AwaitingAcknowledgement::class.java) { runBlocking { f.commands.synchronize(f.account) } }
            assertEquals(1, f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).size)
            f.receipt("already_applied")
            f.commands.synchronize(f.account)
            assertEquals(0, f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).size)
            f.commands.synchronize(f.account)
            assertEquals(0, f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).size)
        } } finally { f.close() }
    }

    @Test fun planAndFinancialConflictsUseTheirOwnPayloadRevisionAndAtomicFailureReceipt(): Unit = runBlocking {
        for (financial in listOf(false, true)) {
            val f = Fixture()
            try { withTimeout(25_000) {
                f.start()
                val collection = if (financial) "transactions" else "scheduled_transactions"
                val id = if (financial) f.financial else f.plan
                val payload = if (financial) f.transactions.normalize(f.raw(true), f.account) else f.schedules.normalize(f.raw(), f.account)
                f.sdk.local.database.syncRecordDao().save(SyncRecord(f.owner, collection, id, pendingPayload = SyncPayload.encode(payload), mutationId = "pending"))
                f.sdk.firestore.collection(collection).document(id).set(f.raw(financial)).await()
                f.receipt("conflict", collection)
                f.commands.synchronize(f.account)
                val row = checkNotNull(f.sdk.local.database.syncRecordDao().get(f.owner, collection, id))
                assertEquals(8L, row.conflictRevision)
                assertEquals(SyncPayload.encode(payload), row.conflictPayload)
                assertEquals("conflict", f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).single().failure)
            } } finally { f.close() }
        }
    }

    @Test fun tombstoneConflictHasNullPayloadAndMissingLocalRecordDoesNotInventFailureState(): Unit = runBlocking {
        val f = Fixture()
        try { withTimeout(25_000) {
            f.start(); f.receipt("conflict", "scheduled_transactions")
            f.sdk.firestore.collection("scheduled_transactions").document(f.plan).set(f.raw() + ("deleted" to true)).await()
            f.commands.synchronize(f.account)
            assertNull(f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).single().failure)
            f.sdk.local.database.syncRecordDao().save(SyncRecord(f.owner, "scheduled_transactions", f.plan, mutationId = "pending"))
            f.commands.synchronize(f.account)
            val row = checkNotNull(f.sdk.local.database.syncRecordDao().get(f.owner, "scheduled_transactions", f.plan))
            assertNull(row.conflictPayload)
            assertEquals(8L, row.conflictRevision)
            assertEquals("conflict", f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).single().failure)
        } } finally { f.close() }
    }

    @Test fun acceptedCompletionKeepsOneCanonicalIdentityWhileNewerFinancialIntentSurvives(): Unit = runBlocking {
        for (newer in listOf(false, true)) {
            val f = Fixture()
            try { withTimeout(25_000) {
                f.sdk.activate(f.owner)
                val canonical = f.transactions.normalize(f.raw(true), f.account)
                val wanted = canonical + ("amountMinor" to if (newer) 2000L else 1000L) + ("amount" to if (newer) 20.0 else 10.0)
                f.start(sent = SyncPayload.encode(canonical))
                val localId = f.sdk.local.database.transactionDao().insertTransaction(TransactionEntity(ownerId = f.owner,
                    firestoreId = f.financial, currencyCode = "USD", amountMinor = if (newer) 2000 else 1000,
                    transaction = TransactionType.EXPENSE, category = CategoryType.FOOD, date = 100))
                f.sdk.local.database.syncRecordDao().save(SyncRecord(f.owner, "transactions", f.financial,
                    pendingPayload = SyncPayload.encode(wanted), mutationId = "current"))
                f.sdk.firestore.collection("transactions").document(f.financial).set(f.raw(true) + ("date" to 200L)).await()
                f.receipt("applied")
                f.commands.synchronize(f.account)
                assertEquals(0, f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).size)
                val rows = f.sdk.local.database.transactionDao().getAllTransactionsOneShot()
                assertEquals(1, rows.size)
                assertEquals(localId.toInt(), rows.single().id)
                val pending = checkNotNull(f.sdk.local.database.syncRecordDao().get(f.owner, "transactions", f.financial))
                if (newer) {
                    assertEquals("current", pending.mutationId)
                    assertEquals(2000L, SyncPayload.decode(checkNotNull(pending.pendingPayload))["amountMinor"])
                    assertEquals(200L, SyncPayload.decode(checkNotNull(pending.pendingPayload))["date"])
                    assertEquals(2000L, rows.single().amountMinor)
                } else {
                    assertNull(pending.mutationId)
                    assertEquals(200L, rows.single().date)
                }
            } } finally { f.close() }
        }
    }

    @Test fun localRollbackKeepsFinancialRowAndCommandWhenAcknowledgementCannotCommit(): Unit = runBlocking {
        val f = Fixture()
        try { withTimeout(25_000) {
            f.start(); f.receipt("conflict", "scheduled_transactions")
            val before = SyncRecord(f.owner, "scheduled_transactions", f.plan, mutationId = "pending")
            f.sdk.local.database.syncRecordDao().save(before)
            f.sdk.firestore.collection("scheduled_transactions").document(f.plan).set(f.raw()).await()
            f.sdk.local.rejectSyncWrites()
            assertThrows(SQLiteException::class.java) { runBlocking { f.commands.synchronize(f.account) } }
            assertEquals(before, f.sdk.local.database.syncRecordDao().get(f.owner, "scheduled_transactions", f.plan))
            assertNull(f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).single().failure)
        } } finally { f.close() }
    }

    @Test fun staleAccountsAndUnknownReceiptsCannotBeAcknowledgedAsSuccess(): Unit = runBlocking {
        val f = Fixture()
        try { withTimeout(25_000) {
            f.start("snooze"); val account = f.account
            f.receipt("unknown_server_outcome")
            f.commands.synchronize(account)
            assertEquals("unknown_server_outcome", f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).single().failure)
            f.sdk.local.database.scheduleCommandDao().insert(ScheduleCommand(f.operation + "-pending", f.owner, f.plan, 100, "snooze", 101))
            f.sdk.activate("other-${UUID.randomUUID()}")
            assertThrows(CancellationException::class.java) { runBlocking { f.commands.synchronize(account) } }
            assertNotNull(f.sdk.local.database.scheduleCommandDao().forAccount(f.owner).first { it.operationId == f.operation }.failure)
        } } finally { f.close() }
    }
}
