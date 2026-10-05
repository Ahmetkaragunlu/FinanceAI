package com.ahmetkaragunlu.financeai.feature.schedule.data

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.toFirebaseMap
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import java.time.Clock
import javax.inject.Inject

/** A single local transaction owns completion, including both durable sync intentions. */
class RoomScheduledCompletion @Inject constructor(
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val pending: PendingChanges,
    private val scheduler: SyncScheduler,
    private val clock: Clock
) : CompleteScheduledTransaction {
    override suspend operator fun invoke(value: ScheduledTransaction): Transaction? = session.withAccount { account ->
        require(value.ownerId == account.ownerId)
        val result = database.withTransaction {
            val scheduled = database.scheduledTransactionDao().getScheduledTransactionById(value.id) ?: return@withTransaction null
            val current = scheduled.toDomain()
            if (scheduled.firestoreId != value.firestoreId) return@withTransaction null
            val remoteId = "completed_${scheduled.firestoreId}"
            val existing = database.transactionDao().getTransactionByFirestoreId(remoteId)
            if (existing != null) return@withTransaction null
            val transaction = Transaction(
                ownerId = account.ownerId, currencyCode = account.currencyCode, firestoreId = remoteId,
                amount = current.amount, transaction = current.type, category = current.category, note = current.note.orEmpty(),
                date = clock.millis(), photoUri = current.photoUri, locationFull = current.locationFull,
                locationShort = current.locationShort, latitude = current.latitude, longitude = current.longitude
            )
            val id = database.transactionDao().insertTransaction(transaction.toEntity())
            database.scheduledTransactionDao().deleteScheduledTransaction(scheduled)
            val payload = transaction.toFirebaseMap().toMutableMap()
            val photoState = database.syncRecordDao().get(account.ownerId, "scheduled_transactions", scheduled.firestoreId)
            val photoMetadata = (photoState?.pendingPayload ?: photoState?.basePayload)?.let(SyncPayload::decode)
                .orEmpty().filterKeys { it in setOf("photoStorageUrl", "photoRemoved", "photoVersion") }
            payload.putAll(photoMetadata)
            if (current.photoUri?.startsWith("https://") == true) payload["photoStorageUrl"] = current.photoUri
            pending.record(account.ownerId, "transactions", remoteId, payload)
            pending.record(account.ownerId, "scheduled_transactions", scheduled.firestoreId, null)
            transaction.copy(id = id.toInt())
        }
        if (result != null) scheduler.enqueue(account.ownerId)
        result
    }
}
