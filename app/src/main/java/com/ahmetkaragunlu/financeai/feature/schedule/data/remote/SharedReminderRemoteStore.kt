package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.ScheduleFields
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.ScheduleStatus
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncFields
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ReminderState
import com.ahmetkaragunlu.financeai.notification.presentation.ReminderPresenter
import javax.inject.Inject
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ScheduleCommandType

/** Read-only server state; local display receipts remain device-specific. */
class SharedReminderRemoteStore @Inject constructor(
    private val database: FinanceDatabase,
    private val scheduler: ReminderScheduler,
    private val presenter: ReminderPresenter
) : RemoteRecordStore {
    override val collection = FirestoreCollections.SCHEDULE_STATES
    override fun normalize(data: Map<String, Any?>, account: ActiveAccount): Map<String, Any?> {
        if (ScheduleStatus.fromWire(data[ScheduleFields.STATUS] as? String) == null ||
            data[ScheduleFields.DATE] !is Number || data[SyncFields.REVISION] !is Number)
            throw DataAccessException.InvalidRemoteData()
        return data.filterKeys { it in setOf(ScheduleFields.STATUS, ScheduleFields.DATE, ScheduleFields.SNOOZE_AT, ScheduleFields.DELETE_AT, SyncFields.REVISION) }
    }
    override suspend fun prepare(account: ActiveAccount, remoteId: String, data: Map<String, Any?>) = data
    override suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?) {
        if (data == null) return
        val dao = database.reminderStateDao()
        val previous = dao.get(account.ownerId, remoteId)
        val revision = (data[SyncFields.REVISION] as Number).toLong()
        if (previous != null && revision < previous.revision) return
        val plan = database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(remoteId)
        if (ScheduleStatus.fromWire(data[ScheduleFields.STATUS] as? String) != ScheduleStatus.ACTIVE) {
            val pending = database.syncRecordDao().get(account.ownerId, FirestoreCollections.SCHEDULED_TRANSACTIONS, remoteId)
            // Preserve a dirty local row/media until generic reconciliation offers the real conflict choice.
            if (plan != null && (pending?.mutationId == null || pending.pendingDelete))
                database.scheduledTransactionDao().deleteScheduledTransaction(plan)
            dao.save((previous ?: ReminderState(account.ownerId, remoteId, (data[ScheduleFields.DATE] as Number).toLong()))
                .copy(active = false, revision = revision))
            scheduler.cancel(account.ownerId, remoteId, plan?.id)
            presenter.cancel(account.ownerId, remoteId)
            return
        }
        val date = (data[ScheduleFields.DATE] as Number).toLong()
        val base = previous?.takeIf { it.scheduledDate == date && it.active } ?: ReminderState(account.ownerId, remoteId, date)
        val pendingSnooze = database.scheduleCommandDao().forRecord(account.ownerId, remoteId)
            .any { ScheduleCommandType.fromWire(it.type) == ScheduleCommandType.SNOOZE && it.failure == null }
        val next = base.copy(snoozeAt = if (pendingSnooze) base.snoozeAt else (data[ScheduleFields.SNOOZE_AT] as? Number)?.toLong(),
            deleteAt = (data[ScheduleFields.DELETE_AT] as? Number)?.toLong(), revision = revision)
        if (next == previous) return
        dao.save(next)
        if (next.snoozeAt != base.snoozeAt && next.snoozeAt != next.consumedSnoozeAt) {
            presenter.cancel(account.ownerId, remoteId)
            scheduler.cancel(account.ownerId, remoteId, plan?.id)
        }
        if (plan != null) scheduler.wake(account.ownerId, remoteId)
    }
}
