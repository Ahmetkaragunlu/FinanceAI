package com.ahmetkaragunlu.financeai.core.sync

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Baseline and durable latest local intention; a conflict never discards either version. */
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

@Dao
interface SyncRecordDao {
    @Query("SELECT * FROM sync_records WHERE ownerId = :ownerId AND collection = :collection AND remoteId = :remoteId")
    suspend fun get(ownerId: String, collection: String, remoteId: String): SyncRecord?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: SyncRecord)
    @Query("SELECT * FROM sync_records WHERE ownerId = :ownerId")
    suspend fun forAccount(ownerId: String): List<SyncRecord>
    @Query("SELECT * FROM sync_records WHERE ownerId = :ownerId AND mutationId IS NOT NULL AND conflictRevision IS NULL AND permanentFailure = 0")
    suspend fun pending(ownerId: String): List<SyncRecord>
    @Query("SELECT * FROM sync_records WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND conflictRevision IS NOT NULL")
    fun observeConflicts(): Flow<List<SyncRecord>>
}
