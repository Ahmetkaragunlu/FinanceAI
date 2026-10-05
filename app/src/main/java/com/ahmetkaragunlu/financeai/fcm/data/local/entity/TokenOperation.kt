package com.ahmetkaragunlu.financeai.fcm.data.local.entity

import androidx.room.Entity

@Entity(tableName = "token_operations", primaryKeys = ["ownerId", "token"])
data class TokenOperation(val ownerId: String, val token: String, val remove: Boolean, val createdAt: Long, val acknowledged: Boolean = false)
