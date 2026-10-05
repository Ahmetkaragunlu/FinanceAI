package com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_commands")
data class ScheduleCommand(
    @PrimaryKey val operationId: String,
    val ownerId: String,
    val remoteId: String,
    val scheduledDate: Long,
    val type: String,
    val requestedAt: Long,
    val planPayload: String? = null,
    val planBasePayload: String? = null,
    val financialPayload: String? = null,
    val failure: String? = null
)
