package com.ahmetkaragunlu.financeai.feature.schedule.data

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncFields
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.PendingChanges
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.toFirebaseMap as scheduledToFirebaseMap
import com.ahmetkaragunlu.financeai.feature.schedule.data.sync.ScheduleCommandQueue
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.completedTransactionId
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ScheduleCommandType
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.toFirebaseMap as transactionToFirebaseMap
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import java.time.Clock
import javax.inject.Inject

/** A single local transaction owns completion, including both durable sync intentions. */
class RoomScheduledCompletion @Inject constructor(
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val pending: PendingChanges,
    private val scheduler: SyncScheduler,
    private val clock: Clock,
    private val commands: ScheduleCommandQueue
) : CompleteScheduledTransaction {
    override suspend operator fun invoke(value: ScheduledTransaction): Transaction? =
        session.withAccount { account ->
            require(value.ownerId == account.ownerId)
            val result = database.withTransaction {
                val scheduled =
                    database.scheduledTransactionDao().getScheduledTransactionById(value.id)
                        ?: return@withTransaction null
                val current = scheduled.toDomain()
                if (scheduled.firestoreId != value.firestoreId) return@withTransaction null
                if (database.reminderStateDao()
                        .get(account.ownerId, scheduled.firestoreId)?.active == false
                )
                    return@withTransaction null
                val remoteId = completedTransactionId(scheduled.firestoreId)
                val existing = database.transactionDao().getTransactionByFirestoreId(remoteId)
                if (existing != null) return@withTransaction null
                val transaction = Transaction(
                    ownerId = account.ownerId,
                    currencyCode = account.currencyCode,
                    firestoreId = remoteId,
                    amount = current.amount,
                    transaction = current.type,
                    category = current.category,
                    note = current.note.orEmpty(),
                    date = clock.millis(),
                    photoUri = current.photoUri,
                    locationFull = current.locationFull,
                    locationShort = current.locationShort,
                    latitude = current.latitude,
                    longitude = current.longitude
                )
                val id = database.transactionDao().insertTransaction(transaction.toEntity())
                database.scheduledTransactionDao().deleteScheduledTransaction(scheduled)
                val payload = transaction.transactionToFirebaseMap().toMutableMap()
                val photoState = database.syncRecordDao().get(
                    account.ownerId,
                    FirestoreCollections.SCHEDULED_TRANSACTIONS,
                    scheduled.firestoreId
                )
                val photoMetadata = (photoState?.pendingPayload ?: photoState?.basePayload)?.let(
                    SyncPayload::decode
                )
                    .orEmpty().filterKeys { it in PhotoFields.PERSISTED_METADATA }
                payload.putAll(photoMetadata)
                if (current.photoUri?.startsWith("https://") == true) payload[PhotoFields.STORAGE_URL] =
                    current.photoUri
                pending.record(
                    account.ownerId,
                    FirestoreCollections.TRANSACTIONS,
                    remoteId,
                    payload
                )
                pending.record(
                    account.ownerId,
                    FirestoreCollections.SCHEDULED_TRANSACTIONS,
                    scheduled.firestoreId,
                    null
                )
                commands.enqueue(
                    account,
                    scheduled.firestoreId,
                    scheduled.scheduledDate,
                    ScheduleCommandType.COMPLETE,
                    current.scheduledToFirebaseMap() + photoMetadata + mapOf(SyncFields.USER_ID to account.ownerId)
                )
                transaction.copy(id = id.toInt())
            }
            if (result != null) scheduler.enqueue(account.ownerId)
            result
        }
}
