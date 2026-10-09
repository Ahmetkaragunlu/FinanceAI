package com.ahmetkaragunlu.financeai.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class AccountSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val coordinator: SessionCoordinator,
    private val engine: AccountSyncEngine
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(AccountWork.OWNER_ID) ?: return Result.failure()
        return try {
            val account = coordinator.activeAccountFor(ownerId) ?: return Result.success()
            engine.synchronize(account)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: FirebaseFirestoreException) {
            if (e.code in setOf(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                    FirebaseFirestoreException.Code.INVALID_ARGUMENT, FirebaseFirestoreException.Code.FAILED_PRECONDITION)) {
                Result.failure()
            } else Result.retry()
        } catch (_: Exception) { Result.retry() }
    }
}
