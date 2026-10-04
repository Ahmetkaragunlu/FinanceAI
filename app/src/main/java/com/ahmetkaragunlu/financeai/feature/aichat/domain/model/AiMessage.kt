package com.ahmetkaragunlu.financeai.feature.aichat.domain.model

import java.util.Date

data class AiMessage(
    val id: Long = 0,
    val text: String,
    val isAi: Boolean,
    val timestamp: Date = Date(),
    val firebaseId: String? = null,
    val isSynced: Boolean = false
)
