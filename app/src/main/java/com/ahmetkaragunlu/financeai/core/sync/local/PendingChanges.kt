package com.ahmetkaragunlu.financeai.core.sync.local

import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingChanges @Inject constructor(private val database: FinanceDatabase) {
    /** Caller owns the same Room transaction as the local row mutation. */
    suspend fun record(ownerId: String, collection: String, remoteId: String, payload: Map<String, Any?>?) {
        require(remoteId.isNotBlank())
        val dao = database.syncRecordDao()
        val previous = dao.get(ownerId, collection, remoteId) ?: SyncRecord(ownerId, collection, remoteId)
        dao.save(previous.copy(
            pendingPayload = payload?.let { values ->
                val photo = previous.pendingPayload ?: previous.basePayload
                val preserved = photo?.let(SyncPayload::decode).orEmpty().filterKeys { it in setOf(PhotoFields.STORAGE_URL, PhotoFields.REMOVED, PhotoFields.VERSION, PhotoFields.INTENT) }
                val defaults = if (collection in setOf(FirestoreCollections.TRANSACTIONS, FirestoreCollections.SCHEDULED_TRANSACTIONS)) mapOf(PhotoFields.STORAGE_URL to null, PhotoFields.REMOVED to false, PhotoFields.VERSION to null) else emptyMap()
                SyncPayload.encode(defaults + preserved + values)
            }, pendingDelete = payload == null,
            mutationId = UUID.randomUUID().toString(), permanentFailure = false
        ))
    }
}
