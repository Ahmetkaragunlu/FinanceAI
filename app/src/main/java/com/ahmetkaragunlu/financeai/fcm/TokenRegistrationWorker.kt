package com.ahmetkaragunlu.financeai.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class TokenRegistrationWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted parameters: WorkerParameters,
    private val manager: FCMTokenManager, private val auth: FirebaseAuth
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val owner = inputData.getString(SyncScheduler.OWNER_ID) ?: auth.currentUser?.uid
        if (owner != null && auth.currentUser?.uid == owner) {
            inputData.getString(TOKEN)?.let { manager.suppliedToken(it) }
            if (inputData.getBoolean(FETCH_CURRENT, false)) manager.updateFCMToken()
            manager.flush(owner)
            if (inputData.getBoolean(FETCH_CURRENT, false)) manager.restorePlans(owner)
        }
        Result.success()
    } catch (e: CancellationException) { throw e }
    catch (_: Exception) { Result.retry() }
    companion object { const val TOKEN = "fcm_token"; const val FETCH_CURRENT = "fetch_current" }
}
