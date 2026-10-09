package com.ahmetkaragunlu.financeai.app

import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.local.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.session.SessionWorkRestorer
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Rebuilds disposable account work from retained local records; WorkManager is not the source of truth. */
class AccountWorkRestorer @Inject constructor(
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val reminders: ReminderScheduler,
    private val photos: PhotoWorkScheduler,
    private val files: PhotoLocalStore,
    private val tokens: FCMTokenManager
) : SessionWorkRestorer {
    override suspend fun restore(account: ActiveAccount) {
        val transactions = database.transactionDao().getAllTransactionsOneShot()
        val schedules = database.scheduledTransactionDao().observeScheduledTransactions().first()
        transactions.filter { it.ownerId == account.ownerId }.forEach {
            upload(account, PhotoRecordType.TRANSACTION, it.firestoreId, it.photoUri)
        }
        schedules.filter { it.ownerId == account.ownerId }.forEach {
            upload(account, PhotoRecordType.SCHEDULED, it.firestoreId, it.photoUri)
            if (session.isCurrent(account)) reminders.wake(account.ownerId, it.firestoreId)
        }
        for (operation in database.photoOperationDao().forAccount(account.ownerId)
            .filter { it.failure == null }) {
            if (session.isCurrent(account)) photos.upload(
                account.ownerId, operation.collection, operation.remoteId, operation.path
            )
        }
        if (session.isCurrent(account)) {
            tokens.restore(account.ownerId)
            files.cleanUnreferenced(account.ownerId)
        }
    }

    private suspend fun upload(
        account: ActiveAccount, recordType: PhotoRecordType, remoteId: String, path: String?
    ) {
        if (session.isCurrent(account) && path != null && File(path).isFile) photos.upload(
            account.ownerId, recordType, remoteId, path
        )
    }
}
