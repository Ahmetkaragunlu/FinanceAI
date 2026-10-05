package com.ahmetkaragunlu.financeai.core.sync.local.entity

import androidx.room.Entity

@Entity(tableName = "sync_records", primaryKeys = ["ownerId", "collection", "remoteId"])
data class SyncRecord(
    val ownerId: String,
    val collection: String,
    val remoteId: String,
    val basePayload: String? = null,
    val baseRevision: Long = 0,
    val pendingPayload: String? = null,
    val pendingDelete: Boolean = false,
    val mutationId: String? = null,
    val conflictPayload: String? = null,
    val conflictRevision: Long? = null,
    val permanentFailure: Boolean = false
)
