package com.ahmetkaragunlu.financeai.feature.aichat.data.remote.message

import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.entity.AiMessageEntity
import java.util.Date
import javax.inject.Inject

class AiMessageRemoteStore @Inject constructor(private val database: FinanceDatabase) :
    RemoteRecordStore {
    override val collection = FirestoreCollections.AI_MESSAGES
    override fun normalize(data: Map<String, Any?>, account: ActiveAccount): Map<String, Any?> =
        mapOf(
            AiMessageFields.TEXT to (data[AiMessageFields.TEXT] as? String ?: ""),
            AiMessageFields.IS_AI to (data[AiMessageFields.IS_AI] as? Boolean ?: false),
            AiMessageFields.TIMESTAMP to ((data[AiMessageFields.TIMESTAMP] as? Number)?.toLong()
                ?: 0L)
        )

    override suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?) {
        val dao = database.aiMessageDao()
        if (data == null) {
            dao.deleteMessageByFirebaseId(remoteId); return
        }
        val existing = dao.getMessageByFirebaseId(remoteId)
        dao.insertMessage(
            AiMessageEntity(
                id = existing?.id ?: 0L,
                ownerId = account.ownerId,
                firebaseId = remoteId,
                text = data[AiMessageFields.TEXT] as String,
                isAi = data[AiMessageFields.IS_AI] as Boolean,
                timestamp = Date((data[AiMessageFields.TIMESTAMP] as Number).toLong()),
                isSynced = true
            )
        )
    }
}
