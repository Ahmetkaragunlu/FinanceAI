package com.ahmetkaragunlu.financeai.feature.aichat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "ai_messages")
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isAi: Boolean,
    val timestamp: Date = Date(),
    val firebaseId: String? = null,
    val isSynced: Boolean = false
)
