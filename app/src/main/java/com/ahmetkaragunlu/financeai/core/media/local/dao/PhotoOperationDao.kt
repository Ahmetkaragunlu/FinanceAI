package com.ahmetkaragunlu.financeai.core.media.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation

@Dao
interface PhotoOperationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(operation: PhotoOperation)
    @Query("SELECT * FROM photo_operations WHERE ownerId = :ownerId")
    suspend fun forAccount(ownerId: String): List<PhotoOperation>
    @Query("DELETE FROM photo_operations WHERE ownerId = :ownerId AND `collection` = :collection AND remoteId = :remoteId AND path = :path")
    suspend fun acknowledge(ownerId: String, collection: String, remoteId: String, path: String)
    @Query("UPDATE photo_operations SET failure = :failure WHERE ownerId = :ownerId AND `collection` = :collection AND remoteId = :remoteId AND path = :path")
    suspend fun fail(ownerId: String, collection: String, remoteId: String, path: String, failure: String)
    @Query("SELECT EXISTS(SELECT 1 FROM transaction_table WHERE photoUri = :path) OR EXISTS(SELECT 1 FROM scheduled_transactions_table WHERE photoUri = :path) OR EXISTS(SELECT 1 FROM photo_operations WHERE path = :path)")
    suspend fun isReferenced(path: String): Boolean
}
