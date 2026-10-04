package com.ahmetkaragunlu.financeai.feature.aichat.data.mapper

import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiMessageEntity
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class AiMessageMapperTest {
    @Test
    fun `chat history preserves user and AI identity ordering timestamps and sync state`() {
        val userMessage = AiMessageEntity(
            id = 5_000_000_000, text = "Bütçemi özetle", isAi = false,
            timestamp = Date(1_750_000_000_000), firebaseId = "remote-message", isSynced = true
        )
        val answer = AiMessageEntity(
            id = 5_000_000_001, text = "Özet", isAi = true, timestamp = Date(1_750_000_000_010)
        )
        val history = listOf(userMessage, answer)

        assertEquals(
            listOf(
                AiMessage(5_000_000_000, "Bütçemi özetle", false, Date(1_750_000_000_000), "remote-message", true),
                AiMessage(5_000_000_001, "Özet", true, Date(1_750_000_000_010))
            ),
            history.map { it.toDomain() }
        )
    }
}
