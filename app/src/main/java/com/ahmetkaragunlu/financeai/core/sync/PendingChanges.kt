package com.ahmetkaragunlu.financeai.core.sync

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
                val preserved = photo?.let(SyncPayload::decode).orEmpty().filterKeys { it == "photoStorageUrl" || it == "photoRemoved" || it == "photoVersion" }
                val defaults = if (collection in setOf("transactions", "scheduled_transactions")) mapOf("photoStorageUrl" to null, "photoRemoved" to false, "photoVersion" to null) else emptyMap()
                SyncPayload.encode(defaults + preserved + values)
            }, pendingDelete = payload == null,
            mutationId = UUID.randomUUID().toString(), permanentFailure = false
        ))
    }
}
