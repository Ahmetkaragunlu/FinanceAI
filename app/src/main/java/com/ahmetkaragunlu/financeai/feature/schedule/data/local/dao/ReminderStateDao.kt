package com.ahmetkaragunlu.financeai.feature.schedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ReminderState

@Dao
interface ReminderStateDao {
    @Query("SELECT * FROM reminder_state WHERE ownerId = :ownerId AND remoteId = :remoteId")
    suspend fun get(ownerId: String, remoteId: String): ReminderState?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: ReminderState)
    @Query("DELETE FROM reminder_state WHERE ownerId = :ownerId AND remoteId = :remoteId")
    suspend fun delete(ownerId: String, remoteId: String)
}
