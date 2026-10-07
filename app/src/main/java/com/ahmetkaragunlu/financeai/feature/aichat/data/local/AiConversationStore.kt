package com.ahmetkaragunlu.financeai.feature.aichat.data.local

import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

interface AiConversationStore {
    fun observe(): Flow<List<AiMessage>>
    suspend fun contains(account: ActiveAccount, messageId: String): Boolean
    suspend fun save(account: ActiveAccount, messageId: String, text: String, isAi: Boolean)
}
