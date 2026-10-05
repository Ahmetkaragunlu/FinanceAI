package com.ahmetkaragunlu.financeai.fcm.data.local.entity

import androidx.room.Entity

@Entity(tableName = "push_events", primaryKeys = ["ownerId", "eventId"])
data class PushEvent(
    val ownerId: String,
    val eventId: String,
    val remoteId: String,
    val type: String,
    val receivedAt: Long,
    val handled: Boolean = false
)
