package com.ahmetkaragunlu.financeai.fcm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.fcm.data.local.entity.PushEvent

@Dao
interface PushEventDao {
    @Query("SELECT * FROM push_events WHERE ownerId = :ownerId AND eventId = :eventId")
    suspend fun get(ownerId: String, eventId: String): PushEvent?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: PushEvent): Long
    @Query("UPDATE push_events SET handled = 1 WHERE ownerId = :ownerId AND eventId = :eventId")
    suspend fun handled(ownerId: String, eventId: String)
    @Query("DELETE FROM push_events WHERE ownerId = :ownerId AND handled = 1 AND receivedAt < :before")
    suspend fun pruneHandled(ownerId: String, before: Long)
}
