package com.ahmetkaragunlu.financeai.core.media.local.entity

import androidx.room.Entity

@Entity(tableName = "photo_operations", primaryKeys = ["ownerId", "collection", "remoteId", "path"])
data class PhotoOperation(val ownerId: String, val collection: String, val remoteId: String,
    val path: String, val version: String, val failure: String? = null)
