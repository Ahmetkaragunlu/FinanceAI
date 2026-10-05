package com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity

import androidx.room.Entity
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderProgress

@Entity(tableName = "reminder_state", primaryKeys = ["ownerId", "remoteId"])
data class ReminderState(
    val ownerId: String,
    val remoteId: String,
    val scheduledDate: Long,
    val automaticSlots: Int = 0,
    val snoozeAt: Long? = null,
    val lastShownAt: Long? = null,
    val expiredShownAt: Long? = null,
    val deleteAt: Long? = null,
    val consumedSnoozeAt: Long? = null,
    val revision: Long = 0,
    val active: Boolean = true
) {
    fun progress() = ReminderProgress(automaticSlots, snoozeAt, lastShownAt, expiredShownAt, deleteAt, consumedSnoozeAt)
}
