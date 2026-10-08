package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.entity.AiMessageEntity
import java.util.Date
import javax.inject.Inject

class AiMessageRemoteStore @Inject constructor(private val database: FinanceDatabase) : RemoteRecordStore {
    override val collection = FirestoreCollections.AI_MESSAGES
    override fun normalize(data: Map<String, Any?>, account: ActiveAccount): Map<String, Any?> = mapOf(
        "text" to (data["text"] as? String ?: ""),
        "isAi" to (data["isAi"] as? Boolean ?: false),
        "timestamp" to ((data["timestamp"] as? Number)?.toLong() ?: 0L)
    )
    override suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?) {
        val dao = database.aiMessageDao()
        if (data == null) { dao.deleteMessageByFirebaseId(remoteId); return }
        val existing = dao.getMessageByFirebaseId(remoteId)
        dao.insertMessage(AiMessageEntity(
            id = existing?.id ?: 0L, ownerId = account.ownerId, firebaseId = remoteId,
            text = data["text"] as String, isAi = data["isAi"] as Boolean,
            timestamp = Date((data["timestamp"] as Number).toLong()), isSynced = true
        ))
    }
}
