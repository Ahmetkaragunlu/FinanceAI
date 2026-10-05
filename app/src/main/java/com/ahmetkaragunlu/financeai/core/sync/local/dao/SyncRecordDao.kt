package com.ahmetkaragunlu.financeai.core.sync.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord

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
