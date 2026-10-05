package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.ahmetkaragunlu.financeai.feature.aichat.data.local.entity.AiMessageEntity

internal fun AiMessageEntity.toFirebaseMap(): Map<String, Any?> = mapOf(
    "text" to text,
    "isAi" to isAi,
    "timestamp" to timestamp.time
)
