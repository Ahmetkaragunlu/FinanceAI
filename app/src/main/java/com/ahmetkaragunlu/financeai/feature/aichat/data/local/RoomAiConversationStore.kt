package com.ahmetkaragunlu.financeai.feature.aichat.data.local

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.local.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.entity.AiMessageEntity
import com.ahmetkaragunlu.financeai.feature.aichat.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.aichat.data.remote.message.toFirebaseMap
import java.time.Clock
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map

class RoomAiConversationStore @Inject constructor(
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val pending: PendingChanges,
    private val scheduler: SyncScheduler,
    private val clock: Clock
) : AiConversationStore {
    override fun observe() = session.observe(emptyList()) {
        database.aiMessageDao().observeMessages().map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun contains(account: ActiveAccount, messageId: String): Boolean =
        session.withAccount { current ->
            if (current != account) throw CancellationException("Stale account")
            database.aiMessageDao().getMessageByFirebaseId(messageId) != null
        }

    override suspend fun save(
        account: ActiveAccount,
        messageId: String,
        text: String,
        isAi: Boolean
    ) {
        session.withAccount { current ->
            if (current != account) throw CancellationException("Stale account")
            database.withTransaction {
                if (database.aiMessageDao().getMessageByFirebaseId(messageId) == null) {
                    val row = AiMessageEntity(
                        ownerId = account.ownerId, firebaseId = messageId,
                        text = text, isAi = isAi, timestamp = Date(clock.millis())
                    )
                    database.aiMessageDao().insertMessage(row)
                    pending.record(
                        account.ownerId,
                        FirestoreCollections.AI_MESSAGES,
                        messageId,
                        row.toFirebaseMap()
                    )
                }
            }
            scheduler.enqueue(account.ownerId)
        }
    }
}
