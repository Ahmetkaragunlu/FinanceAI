package com.ahmetkaragunlu.financeai.feature.schedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduleCommand

@Dao
interface ScheduleCommandDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: ScheduleCommand): Long
    @Query("SELECT * FROM schedule_commands WHERE ownerId = :ownerId ORDER BY requestedAt, operationId")
    suspend fun forAccount(ownerId: String): List<ScheduleCommand>
    @Query("SELECT * FROM schedule_commands WHERE ownerId = :ownerId AND remoteId = :remoteId ORDER BY requestedAt")
    suspend fun forRecord(ownerId: String, remoteId: String): List<ScheduleCommand>
    @Query("DELETE FROM schedule_commands WHERE operationId = :id")
    suspend fun acknowledge(id: String)
    @Query("UPDATE schedule_commands SET failure = :reason WHERE operationId = :id")
    suspend fun fail(id: String, reason: String)
}
