package com.ahmetkaragunlu.financeai.feature.schedule.data.reminder

import com.ahmetkaragunlu.financeai.feature.schedule.data.sync.ScheduleCommandQueue
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ReminderState
import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ScheduleCommandType
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderDecision
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKind
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderPolicy
import com.ahmetkaragunlu.financeai.notification.presentation.ReminderPresenter
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Local state + owner lock serialize local and FCM wake-ups; neither may bypass the policy. */
class ReminderCoordinator @Inject constructor(
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val commands: ScheduleCommandQueue,
    private val sync: SyncScheduler,
    private val scheduler: ReminderScheduler,
    private val presenter: ReminderPresenter,
    private val clock: Clock
) {
    suspend fun process(account: ActiveAccount, remoteId: String) {
        var next: Long? = null
        var removed = false
        session.withAccount { current ->
            check(current == account)
            database.withTransaction {
                val plan = database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(remoteId)
                if (plan == null) {
                    database.reminderStateDao().delete(account.ownerId, remoteId)
                    presenter.cancel(account.ownerId, remoteId)
                    removed = true
                    return@withTransaction
                }
                val dao = database.reminderStateDao()
                val previous = dao.get(account.ownerId, remoteId)
                var state = previous?.takeIf { it.scheduledDate == plan.scheduledDate }
                    ?: ReminderState(account.ownerId, remoteId, plan.scheduledDate)
                if (!state.active) {
                    presenter.cancel(account.ownerId, remoteId)
                    scheduler.cancel(account.ownerId, remoteId, plan.id)
                    return@withTransaction
                }
                val now = clock.millis()
                val zone = ZoneId.of(account.timeZoneId)
                when (val decision = ReminderPolicy.next(plan.scheduledDate, state.progress(), now, zone)) {
                    is ReminderDecision.Wait -> next = decision.until
                    is ReminderDecision.Skip -> {
                        state = state.copy(automaticSlots = state.automaticSlots or decision.consumedSlots)
                        next = (ReminderPolicy.next(plan.scheduledDate, state.progress(), now, zone) as? ReminderDecision.Wait)?.until
                    }
                    is ReminderDecision.Show -> {
                        val event = "${plan.scheduledDate}/${decision.kind}/${if (decision.kind == ReminderKind.SNOOZE) state.snoozeAt else 0}"
                        if (presenter.show(plan.toDomain(), decision.kind, event)) {
                            state = state.copy(automaticSlots = state.automaticSlots or decision.consumedSlots,
                                consumedSnoozeAt = if (decision.kind == ReminderKind.SNOOZE) state.snoozeAt else state.consumedSnoozeAt,
                                lastShownAt = now,
                                expiredShownAt = if (decision.kind == ReminderKind.EXPIRED) now else state.expiredShownAt)
                            if (decision.kind == ReminderKind.EXPIRED) {
                                commands.enqueue(account, remoteId, plan.scheduledDate, ScheduleCommandType.EXPIRATION_SHOWN)
                                sync.enqueue(account.ownerId)
                            }
                            val after = ReminderPolicy.next(plan.scheduledDate, state.progress(), now, zone)
                            next = (after as? ReminderDecision.Wait)?.until
                        }
                        // Permission/channel denial leaves the intent pending; resume will restore it.
                    }
                    ReminderDecision.AwaitRemote -> sync.enqueue(account.ownerId)
                }
                if (!removed) dao.save(state)
            }
        }
        if (removed) { scheduler.cancel(account.ownerId, remoteId); sync.enqueue(account.ownerId) }
        else if (session.isCurrent(account)) next?.let { scheduler.wake(account.ownerId, remoteId, it) }
    }

    suspend fun snooze(account: ActiveAccount, remoteId: String, requestedAt: Long = clock.millis()): Long? {
        var due: Long? = null
        session.withAccount { current ->
            check(current == account)
            database.withTransaction {
                val plan = database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(remoteId) ?: return@withTransaction
                val old = database.reminderStateDao().get(account.ownerId, remoteId)?.takeIf { it.scheduledDate == plan.scheduledDate }
                    ?: ReminderState(account.ownerId, remoteId, plan.scheduledDate)
                if (old.expiredShownAt != null) return@withTransaction
                if (!old.active) return@withTransaction
                if (requestedAt < 0 || requestedAt > clock.millis() + 300_000) return@withTransaction
                due = Math.addExact(requestedAt, ReminderPolicy.snoozeDuration.toMillis())
                database.reminderStateDao().save(old.copy(snoozeAt = due))
                commands.enqueue(account, remoteId, plan.scheduledDate, ScheduleCommandType.SNOOZE, requestedAt = requestedAt)
                presenter.cancel(account.ownerId, remoteId)
            }
        }
        if (session.isCurrent(account) && due != null) {
            sync.enqueue(account.ownerId)
            scheduler.wake(account.ownerId, remoteId)
        }
        return due
    }
    fun dismiss(account: ActiveAccount, remoteId: String) {
        if (session.isCurrent(account)) presenter.cancel(account.ownerId, remoteId)
    }
    suspend fun restoreCurrent() {
        val account = session.account.value ?: return
        val plans = database.scheduledTransactionDao().observeScheduledTransactions().first()
        if (session.isCurrent(account)) plans.filter { it.ownerId == account.ownerId }.forEach { scheduler.wake(account.ownerId, it.firestoreId) }
    }
}
