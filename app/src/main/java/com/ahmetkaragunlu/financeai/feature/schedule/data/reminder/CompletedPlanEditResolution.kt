package com.ahmetkaragunlu.financeai.feature.schedule.data.reminder

import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.Reconciliation
import com.ahmetkaragunlu.financeai.core.sync.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.ScheduledTransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import javax.inject.Inject

internal data class CompletedFinancialSnapshot(val remoteId: String, val payload: String?, val revision: Long,
    val prepared: Map<String, Any?>?)

/** Caller owns the account guard and Room transaction. No network or image processing here. */
class CompletedPlanEditResolution @Inject constructor(
    private val database: FinanceDatabase, private val pending: PendingChanges,
    private val transactions: TransactionRemoteStore, private val schedules: ScheduledTransactionRemoteStore,
    private val photos: PhotoWorkScheduler
) {
    internal suspend fun apply(account: ActiveAccount, conflict: SyncRecord, keepLocal: Boolean,
        financial: CompletedFinancialSnapshot): Boolean {
        val desiredPlan = conflict.pendingPayload
        if (conflict.pendingDelete || desiredPlan == null) return false
        if (keepLocal) {
            val remote = checkNotNull(financial.payload) { "Completed transaction is unavailable" }
            val dao = database.syncRecordDao()
            val old = dao.get(account.ownerId, "transactions", financial.remoteId)
            val projected = projectCompletedPlanEdit(conflict.basePayload, desiredPlan, remote,
                old?.basePayload, old?.takeIf { it.mutationId != null && !it.pendingDelete }?.pendingPayload)
            val wanted = (projected.decision as? Reconciliation.Write)?.payload ?: projected.wanted
            val localPlan = database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(conflict.remoteId)
            val localFinancial = database.transactionDao().getTransactionByFirestoreId(financial.remoteId)
            val oldPlan = conflict.basePayload?.let(SyncPayload::decode).orEmpty()
            val newPlan = SyncPayload.decode(desiredPlan)
            val planPhotoChanged = listOf(PhotoFields.INTENT, PhotoFields.REMOVED, PhotoFields.STORAGE_URL)
                .any { oldPlan[it] != newPlan[it] }
            val localPhoto = if (planPhotoChanged) localPlan?.photoUri
                else if (old?.mutationId != null) localFinancial?.photoUri else financial.prepared?.get("localPhotoUri")
            val values = SyncPayload.decode(wanted)
            transactions.apply(account, financial.remoteId, values + mapOf("localPhotoUri" to localPhoto))
            val base = if (projected.decision == Reconciliation.Conflict) projected.base else remote
            dao.save(SyncRecord(account.ownerId, "transactions", financial.remoteId, base, financial.revision))
            if (projected.decision != Reconciliation.Equal) pending.record(account.ownerId, "transactions", financial.remoteId, values)
            if (projected.decision == Reconciliation.Conflict) {
                val queued = checkNotNull(dao.get(account.ownerId, "transactions", financial.remoteId))
                dao.save(queued.copy(conflictPayload = remote, conflictRevision = financial.revision))
            }
            val row = database.transactionDao().getTransactionByFirestoreId(financial.remoteId)
            photos.upload(account.ownerId, "transactions", financial.remoteId, row?.photoUri)
        } else {
            val old = database.syncRecordDao().get(account.ownerId, "transactions", financial.remoteId)
            if (old?.mutationId == null) {
                transactions.apply(account, financial.remoteId, financial.prepared)
                database.syncRecordDao().save(SyncRecord(account.ownerId, "transactions", financial.remoteId,
                    financial.payload, financial.revision))
            }
        }
        schedules.apply(account, conflict.remoteId, null)
        database.syncRecordDao().save(SyncRecord(account.ownerId, "scheduled_transactions", conflict.remoteId,
            null, checkNotNull(conflict.conflictRevision)))
        return true
    }
}
