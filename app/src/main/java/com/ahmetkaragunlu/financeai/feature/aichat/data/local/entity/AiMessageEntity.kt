package com.ahmetkaragunlu.financeai.feature.aichat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "ai_messages", indices = [Index(value = ["ownerId", "firebaseId"], unique = true)])
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isAi: Boolean,
    val timestamp: Date = Date(),
    val firebaseId: String? = null,
    val ownerId: String = "",
    val isSynced: Boolean = false
)
