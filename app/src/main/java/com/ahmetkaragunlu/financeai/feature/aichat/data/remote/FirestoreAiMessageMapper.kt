package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.ahmetkaragunlu.financeai.feature.aichat.data.local.entity.AiMessageEntity

internal fun AiMessageEntity.toFirebaseMap(): Map<String, Any?> = mapOf(
    AiMessageFields.TEXT to text,
    AiMessageFields.IS_AI to isAi,
    AiMessageFields.TIMESTAMP to timestamp.time
)
