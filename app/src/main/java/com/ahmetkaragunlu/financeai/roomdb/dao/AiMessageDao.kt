package com.ahmetkaragunlu.financeai.roomdb.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {

    @Query("SELECT * FROM ai_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<AiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity): Long

    @Query("UPDATE ai_messages SET isSynced = 1, firebaseId = :firebaseId WHERE id = :localId")
    suspend fun updateSyncStatus(localId: Long, firebaseId: String)

    @Query("DELETE FROM ai_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM ai_messages WHERE firebaseId = :firebaseId")
    suspend fun deleteMessageByFirebaseId(firebaseId: String)
}