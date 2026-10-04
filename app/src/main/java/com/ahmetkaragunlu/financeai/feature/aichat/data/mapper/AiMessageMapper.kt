package com.ahmetkaragunlu.financeai.feature.aichat.data.mapper

import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiMessageEntity
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage

fun AiMessageEntity.toDomain(): AiMessage = AiMessage(
    id = id,
    text = text,
    isAi = isAi,
    timestamp = timestamp,
    firebaseId = firebaseId,
    isSynced = isSynced
)
