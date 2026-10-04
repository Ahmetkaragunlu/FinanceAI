package com.ahmetkaragunlu.financeai.feature.aichat.domain.repository

import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    fun observeChatHistory(): Flow<List<AiMessage>>
    suspend fun sendMessage(userMessage: String)
}
