package com.ahmetkaragunlu.financeai.feature.aichat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {

    @Query("SELECT * FROM ai_messages WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) ORDER BY timestamp ASC")
    fun observeMessages(): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity): Long

    @Query("SELECT * FROM ai_messages WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND firebaseId = :firebaseId LIMIT 1")
    suspend fun getMessageByFirebaseId(firebaseId: String): AiMessageEntity?

    @Query("DELETE FROM ai_messages WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND firebaseId = :firebaseId")
    suspend fun deleteMessageByFirebaseId(firebaseId: String)
}
