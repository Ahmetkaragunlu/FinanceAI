package com.ahmetkaragunlu.financeai.app

import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.schedule.data.RoomScheduledCompletion
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.notification.presentation.ReminderPresenter
import javax.inject.Inject

/** Screen and notification actions share local completion and the same disposable work handoff. */
class ScheduledCompletionCoordinator @Inject constructor(
    private val completeLocally: RoomScheduledCompletion,
    private val session: AccountSession,
    private val reminders: ReminderScheduler,
    private val presenter: ReminderPresenter,
    private val photos: PhotoWorkScheduler
) : CompleteScheduledTransaction {
    override suspend fun invoke(value: ScheduledTransaction): Transaction? {
        val account = session.requireAccount()
        val completed = completeLocally(value) ?: return null
        if (session.isCurrent(account)) {
            reminders.cancel(account.ownerId, value.firestoreId, value.id)
            presenter.cancel(account.ownerId, value.firestoreId)
            photos.upload(completed.ownerId, PhotoRecordType.TRANSACTION, completed.firestoreId, completed.photoUri)
        }
        return completed
    }
}
