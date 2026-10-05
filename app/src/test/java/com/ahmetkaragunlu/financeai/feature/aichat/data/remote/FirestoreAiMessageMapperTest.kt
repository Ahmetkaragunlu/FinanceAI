package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiMessageEntity
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreAiMessageMapperTest {
    @Test
    fun `message timestamp stays in milliseconds without local persistence metadata`() {
        val message = AiMessageEntity(id = 31, text = "Saved message", isAi = true,
            timestamp = Date(1_750_000_000_125L), firebaseId = "remote-message", isSynced = true)

        assertEquals(
            mapOf("text" to "Saved message", "isAi" to true, "timestamp" to 1_750_000_000_125L),
            message.toFirebaseMap()
        )
    }
}
