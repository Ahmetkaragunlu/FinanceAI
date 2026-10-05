package com.ahmetkaragunlu.financeai.app

import androidx.work.WorkManager
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.schedule.data.RoomScheduledCompletion
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.photo.PhotoWorkScheduler
import javax.inject.Inject

/** Screen and notification actions share local completion and the same disposable work handoff. */
class ScheduledCompletionCoordinator @Inject constructor(
    private val completeLocally: RoomScheduledCompletion,
    private val session: AccountSession,
    private val workManager: WorkManager,
    private val photos: PhotoWorkScheduler
) : CompleteScheduledTransaction {
    override suspend fun invoke(value: ScheduledTransaction): Transaction? {
        val account = session.requireAccount()
        val completed = completeLocally(value) ?: return null
        if (session.isCurrent(account)) {
            workManager.cancelAllWorkByTag("scheduled_notification_${value.id}")
            workManager.cancelAllWorkByTag("delete_expired_${value.id}")
            photos.upload(completed.ownerId, "transactions", completed.firestoreId, completed.photoUri)
        }
        return completed
    }
}
