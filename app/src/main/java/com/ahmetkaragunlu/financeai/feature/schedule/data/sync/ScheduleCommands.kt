package com.ahmetkaragunlu.financeai.feature.schedule.data.sync

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.AccountSyncParticipant
import com.ahmetkaragunlu.financeai.core.sync.contract.LocalConflictResolution
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncFields
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduleCommand
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.ScheduledTransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.CommandOutcome
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.ScheduleCommandFields
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.ScheduleFields
import com.ahmetkaragunlu.financeai.feature.schedule.domain.error.ScheduleException
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.completedTransactionId
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.planIdOf
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ScheduleCommandType
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionFields
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class ScheduleCommands @Inject constructor(
    private val database: FinanceDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val session: AccountSession,
    private val schedules: ScheduledTransactionRemoteStore,
    private val transactions: TransactionRemoteStore,
    private val clock: Clock,
    private val completedEdits: CompletedPlanEditResolution
) : AccountSyncParticipant {
    override suspend fun heldRecords(account: ActiveAccount): Set<Pair<String, String>> =
        database.scheduleCommandDao().forAccount(account.ownerId).filter { ScheduleCommandType.fromWire(it.type) == ScheduleCommandType.COMPLETE }
            .flatMap { listOf(FirestoreCollections.SCHEDULED_TRANSACTIONS to it.remoteId, FirestoreCollections.TRANSACTIONS to completedTransactionId(it.remoteId)) }.toSet()

    override suspend fun prepareResolution(account: ActiveAccount, conflict: SyncRecord, keepLocal: Boolean,
        remoteDocument: Map<String, Any?>): LocalConflictResolution? {
        if (conflict.collection != FirestoreCollections.SCHEDULED_TRANSACTIONS || conflict.pendingDelete || remoteDocument[SyncFields.DELETED] != true) return null
        val id = completedTransactionId(conflict.remoteId)
        val document = firestore.collection(FirestoreCollections.TRANSACTIONS).document(id).get(Source.SERVER).await()
        ensureCurrent(account)
        if (!document.exists() && remoteDocument[ScheduleFields.COMPLETED_FROM] == null) return null
        require(!document.exists() || document.getString(SyncFields.USER_ID) == account.ownerId)
        val payload = if (document.exists() && document.getBoolean(SyncFields.DELETED) != true)
            transactions.normalize(document.data.orEmpty(), account) else null
        val prepared = payload?.let { transactions.prepare(account, id, it) }
        val snapshot = CompletedFinancialSnapshot(id, payload?.let(SyncPayload::encode), document.getLong(SyncFields.REVISION) ?: 0, prepared)
        return LocalConflictResolution { current -> completedEdits.apply(account, current, keepLocal, snapshot) }
    }

    override suspend fun synchronize(account: ActiveAccount) {
        for (command in database.scheduleCommandDao().forAccount(account.ownerId).filter { it.failure == null }) {
            ensureCurrent(account)
            val ref = firestore.collection(FirestoreCollections.SCHEDULE_COMMANDS).document(command.operationId)
            firestore.runTransaction { transaction ->
                val existing = transaction.get(ref)
                if (!existing.exists()) {
                    transaction.set(ref, mapOf(SyncFields.USER_ID to command.ownerId, ScheduleCommandFields.TRANSACTION_ID to command.remoteId,
                        ScheduleFields.DATE to command.scheduledDate, ScheduleCommandFields.TYPE to command.type,
                        ScheduleCommandFields.REQUESTED_AT to command.requestedAt, ScheduleCommandFields.PROCESSED to false,
                        ScheduleCommandFields.PLAN to command.planPayload?.let(SyncPayload::decode),
                        ScheduleCommandFields.BASE to command.planBasePayload?.let(SyncPayload::decode)))
                } else require(existing.getString(SyncFields.USER_ID) == account.ownerId)
            }.await()
            val receipt = ref.get(Source.SERVER).await()
            ensureCurrent(account)
            if (receipt.getBoolean(ScheduleCommandFields.PROCESSED) != true) throw ScheduleException.AwaitingAcknowledgement()
            val rawOutcome = receipt.getString(ScheduleCommandFields.OUTCOME) ?: "invalid"
            val outcome = CommandOutcome.fromWire(rawOutcome)
            val type = ScheduleCommandType.fromWire(command.type)
            if (type == null) {
                database.scheduleCommandDao().fail(command.operationId, "invalid_command")
                continue
            }
            if (outcome == CommandOutcome.CONFLICT && type == ScheduleCommandType.COMPLETE) {
                if (receipt.getString(ScheduleCommandFields.CONFLICT_TARGET) == FirestoreCollections.TRANSACTIONS) markFinancialConflict(account, command)
                else markConflict(account, command)
                continue
            }
            if (outcome?.acknowledgesCommand != true) {
                database.scheduleCommandDao().fail(command.operationId, rawOutcome)
                continue
            }
            if (type == ScheduleCommandType.COMPLETE) acknowledgeCompletion(account, command)
            else session.withAccount { current ->
                check(current == account)
                database.scheduleCommandDao().acknowledge(command.operationId)
            }
        }
    }

    private suspend fun acknowledgeCompletion(account: ActiveAccount, command: ScheduleCommand) {
        val id = completedTransactionId(command.remoteId)
        val document = firestore.collection(FirestoreCollections.TRANSACTIONS).document(id).get(Source.SERVER).await()
        val accepted = document.exists() && document.getString(SyncFields.USER_ID) == account.ownerId
        val payload = if (accepted && document.getBoolean(SyncFields.DELETED) != true)
            transactions.normalize(document.data.orEmpty(), account) else null
        val prepared = payload?.let { transactions.prepare(account, id, it) }
        val plan = firestore.collection(FirestoreCollections.SCHEDULED_TRANSACTIONS).document(command.remoteId).get(Source.SERVER).await()
        val remaining = if (plan.exists() && plan.getBoolean(SyncFields.DELETED) != true)
            schedules.normalize(plan.data.orEmpty(), account) else null
        val preparedPlan = remaining?.let { schedules.prepare(account, command.remoteId, it) }
        session.withAccount { current ->
            check(current == account)
            database.withTransaction {
                // First remote winner is canonical, including its date/money/photo metadata.
                // A terminal rejection removes only the provisional local completion, never another record.
                val dao = database.syncRecordDao()
                val latest = dao.get(account.ownerId, FirestoreCollections.TRANSACTIONS, id) ?: SyncRecord(account.ownerId, FirestoreCollections.TRANSACTIONS, id)
                val canonical = payload?.let(SyncPayload::encode)
                val acknowledgement = acknowledgeCompletion(latest, command.financialPayload, canonical, document.getLong(SyncFields.REVISION) ?: 0)
                if (acknowledgement.applyRemote) transactions.apply(account, id, prepared)
                dao.save(acknowledgement.record)
                schedules.apply(account, command.remoteId, preparedPlan)
                database.syncRecordDao().save(SyncRecord(account.ownerId, FirestoreCollections.SCHEDULED_TRANSACTIONS, command.remoteId,
                    remaining?.let(SyncPayload::encode), plan.getLong(SyncFields.REVISION) ?: 0))
                database.scheduleCommandDao().acknowledge(command.operationId)
            }
        }
    }

    private suspend fun markConflict(account: ActiveAccount, command: ScheduleCommand) {
        val document = firestore.collection(FirestoreCollections.SCHEDULED_TRANSACTIONS).document(command.remoteId).get(Source.SERVER).await()
        require(document.getString(SyncFields.USER_ID) == account.ownerId)
        val payload = if (document.getBoolean(SyncFields.DELETED) == true) null
            else SyncPayload.encode(schedules.normalize(document.data.orEmpty(), account))
        session.withAccount { current ->
            check(current == account)
            database.withTransaction {
                val record = database.syncRecordDao().get(account.ownerId, FirestoreCollections.SCHEDULED_TRANSACTIONS, command.remoteId) ?: return@withTransaction
                database.syncRecordDao().save(record.copy(conflictPayload = payload, conflictRevision = document.getLong(SyncFields.REVISION) ?: 0))
                database.scheduleCommandDao().fail(command.operationId, CommandOutcome.CONFLICT.wireValue)
            }
        }
    }

    private suspend fun markFinancialConflict(account: ActiveAccount, command: ScheduleCommand) {
        val id = completedTransactionId(command.remoteId)
        val document = firestore.collection(FirestoreCollections.TRANSACTIONS).document(id).get(Source.SERVER).await()
        require(document.getString(SyncFields.USER_ID) == account.ownerId)
        val payload = if (document.getBoolean(SyncFields.DELETED) == true) null
            else SyncPayload.encode(transactions.normalize(document.data.orEmpty(), account))
        session.withAccount { current ->
            check(current == account)
            database.withTransaction {
                val record = database.syncRecordDao().get(account.ownerId, FirestoreCollections.TRANSACTIONS, id) ?: return@withTransaction
                database.syncRecordDao().save(record.copy(conflictPayload = payload, conflictRevision = document.getLong(SyncFields.REVISION) ?: 0))
                database.scheduleCommandDao().fail(command.operationId, CommandOutcome.CONFLICT.wireValue)
            }
        }
    }

    override suspend fun resolve(account: ActiveAccount, conflict: SyncRecord, keepLocal: Boolean,
        remotePayload: String?, prepared: Map<String, Any?>?, revision: Long): Boolean {
        val planId = planIdOf(conflict.remoteId)
        if (conflict.collection == FirestoreCollections.TRANSACTIONS && planId != null) {
            val command = database.scheduleCommandDao().forRecord(account.ownerId, planId)
                .firstOrNull { ScheduleCommandType.fromWire(it.type) == ScheduleCommandType.COMPLETE && CommandOutcome.fromWire(it.failure) == CommandOutcome.CONFLICT } ?: return false
            if (keepLocal) {
                val canonical = checkNotNull(remotePayload) { "Completed transaction is unavailable" }
                val wanted = conflict.pendingPayload?.let(SyncPayload::decode)?.toMutableMap()
                if (!conflict.pendingDelete && wanted != null) {
                    wanted[TransactionFields.DATE] = SyncPayload.decode(canonical)[TransactionFields.DATE]
                    val row = database.transactionDao().getTransactionByFirestoreId(conflict.remoteId)
                    transactions.apply(account, conflict.remoteId, wanted + mapOf(PhotoFields.LOCAL_URI to row?.photoUri))
                }
                database.syncRecordDao().save(conflict.copy(basePayload = canonical, baseRevision = revision,
                    pendingPayload = wanted?.let(SyncPayload::encode), conflictPayload = null, conflictRevision = null))
            } else {
                transactions.apply(account, conflict.remoteId, prepared)
                database.syncRecordDao().save(SyncRecord(account.ownerId, FirestoreCollections.TRANSACTIONS, conflict.remoteId, remotePayload, revision))
            }
            schedules.apply(account, planId, null)
            database.syncRecordDao().save(SyncRecord(account.ownerId, FirestoreCollections.SCHEDULED_TRANSACTIONS, planId))
            database.scheduleCommandDao().acknowledge(command.operationId)
            return true
        }
        if (conflict.collection != FirestoreCollections.SCHEDULED_TRANSACTIONS) return false
        val dao = database.scheduleCommandDao()
        val command = dao.forRecord(account.ownerId, conflict.remoteId)
            .firstOrNull { ScheduleCommandType.fromWire(it.type) == ScheduleCommandType.COMPLETE && CommandOutcome.fromWire(it.failure) == CommandOutcome.CONFLICT } ?: return false
        if (keepLocal) {
            val date = remotePayload?.let(SyncPayload::decode)?.get(ScheduleFields.DATE) as? Number
            dao.insert(command.copy(operationId = UUID.randomUUID().toString(), planBasePayload = remotePayload,
                scheduledDate = date?.toLong() ?: command.scheduledDate, requestedAt = clock.millis(), failure = null))
            database.syncRecordDao().save(conflict.copy(basePayload = remotePayload, baseRevision = revision,
                conflictPayload = null, conflictRevision = null))
        } else {
            val id = completedTransactionId(command.remoteId)
            transactions.apply(account, id, null)
            database.syncRecordDao().save(SyncRecord(account.ownerId, FirestoreCollections.TRANSACTIONS, id))
            schedules.apply(account, command.remoteId, prepared)
            database.syncRecordDao().save(SyncRecord(account.ownerId, FirestoreCollections.SCHEDULED_TRANSACTIONS, command.remoteId,
                remotePayload, revision))
        }
        dao.acknowledge(command.operationId)
        return true
    }

    private fun ensureCurrent(account: ActiveAccount) {
        if (!session.isCurrent(account) || auth.currentUser?.uid != account.ownerId)
            throw CancellationException("Stale account")
    }
}
