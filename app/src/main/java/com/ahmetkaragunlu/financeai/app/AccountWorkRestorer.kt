package com.ahmetkaragunlu.financeai.app

import androidx.work.*
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.session.SessionWorkRestorer
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.notification.NotificationWorker
import com.ahmetkaragunlu.financeai.photo.PhotoWorkScheduler
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Rebuilds disposable account work from retained local records; WorkManager is not the source of truth. */
class AccountWorkRestorer @Inject constructor(
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val workManager: WorkManager,
    private val photos: PhotoWorkScheduler
) : SessionWorkRestorer {
    override suspend fun restore(account: ActiveAccount) {
        val transactions = database.transactionDao().getAllTransactionsOneShot()
        val schedules = database.scheduledTransactionDao().observeScheduledTransactions().first()
        transactions.filter { it.ownerId == account.ownerId }.forEach {
            upload(account, "transactions", it.firestoreId, it.photoUri)
        }
        schedules.filter { it.ownerId == account.ownerId }.forEach {
            upload(account, "scheduled", it.firestoreId, it.photoUri)
            if (session.isCurrent(account) && it.scheduledDate > System.currentTimeMillis()) {
                val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInputData(workDataOf(NotificationWorker.TRANSACTION_ID_KEY to it.id, SyncScheduler.OWNER_ID to account.ownerId))
                    .setInitialDelay(5, TimeUnit.SECONDS)
                    .addTag("account_${account.ownerId}").addTag("scheduled_notification_${it.id}").build()
                workManager.enqueueUniqueWork("scheduled_notification_${it.id}", ExistingWorkPolicy.KEEP, request)
            }
        }
    }
    private fun upload(account: ActiveAccount, collection: String, remoteId: String, path: String?) {
        if (session.isCurrent(account) && path != null && File(path).isFile) photos.upload(account.ownerId, collection, remoteId, path)
    }
}
