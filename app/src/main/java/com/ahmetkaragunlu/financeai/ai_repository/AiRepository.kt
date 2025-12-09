package com.ahmetkaragunlu.financeai.ai_repository

import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    fun getChatHistory(): Flow<List<AiMessageEntity>>
    suspend fun sendMessage(userMessage: String)
}