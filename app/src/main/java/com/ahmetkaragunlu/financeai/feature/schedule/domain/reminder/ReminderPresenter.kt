package com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder

import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction

/** Android presentation implements these schedule-owned display/cancellation decisions. */
interface ReminderPresenter {
    fun show(plan: ScheduledTransaction, kind: ReminderKind, eventId: String): Boolean

    fun cancel(ownerId: String, remoteId: String)
}
