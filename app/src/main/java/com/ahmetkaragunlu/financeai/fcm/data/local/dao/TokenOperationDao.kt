package com.ahmetkaragunlu.financeai.fcm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.fcm.data.local.entity.TokenOperation

@Dao
interface TokenOperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: TokenOperation)
    @Query("SELECT * FROM token_operations WHERE ownerId = :ownerId AND acknowledged = 0 ORDER BY createdAt")
    suspend fun pending(ownerId: String): List<TokenOperation>
    @Query("SELECT * FROM token_operations WHERE ownerId = :ownerId AND remove = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestRegistered(ownerId: String): TokenOperation?
    @Query("UPDATE token_operations SET acknowledged = 1 WHERE ownerId = :ownerId AND token = :token AND createdAt = :createdAt AND remove = :remove")
    suspend fun acknowledge(ownerId: String, token: String, createdAt: Long, remove: Boolean)
}
