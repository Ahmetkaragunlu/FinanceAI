package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/** Persisted legacy identity. Restore the current intent; never delete a potentially referenced remote photo. */
@HiltWorker
class PhotoMoveWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted parameters: WorkerParameters,
    private val sessions: SessionCoordinator, private val database: FinanceDatabase, private val photos: PhotoWorkScheduler
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.failure()
        val record = inputData.getString(KEY_TRANSACTION_ID) ?: return Result.failure()
        return try {
            sessions.prepare()
            val account = sessions.session.account.value?.takeIf { it.ownerId == owner } ?: return Result.success()
            val row = database.transactionDao().getTransactionByFirestoreId(record) ?: return Result.success()
            if (sessions.session.isCurrent(account)) photos.upload(owner, PhotoRecordType.TRANSACTION, record, row.photoUri)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
    companion object { const val KEY_TRANSACTION_ID = "transaction_id" }
}
