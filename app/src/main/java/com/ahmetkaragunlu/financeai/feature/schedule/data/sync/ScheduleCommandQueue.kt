package com.ahmetkaragunlu.financeai.feature.schedule.data.sync

import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduleCommand
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.completedTransactionId
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ScheduleCommandType
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/** Called inside the transaction that stores the local action. No network dependency here. */
class ScheduleCommandQueue @Inject constructor(private val database: FinanceDatabase, private val clock: Clock) {
    suspend fun enqueue(account: ActiveAccount, remoteId: String, scheduledDate: Long, type: ScheduleCommandType,
        plan: Map<String, Any?>? = null, requestedAt: Long = clock.millis()) {
        val base = if (type == ScheduleCommandType.COMPLETE) database.syncRecordDao().get(account.ownerId, FirestoreCollections.SCHEDULED_TRANSACTIONS, remoteId)?.basePayload else null
        val financial = if (type == ScheduleCommandType.COMPLETE) database.syncRecordDao().get(account.ownerId, FirestoreCollections.TRANSACTIONS, completedTransactionId(remoteId))?.pendingPayload else null
        database.scheduleCommandDao().insert(ScheduleCommand(UUID.randomUUID().toString(), account.ownerId,
            remoteId, scheduledDate, type.wireValue, requestedAt, plan?.let(SyncPayload::encode), base, financial))
    }
}
