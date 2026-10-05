package com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class ReminderProgress(
    val automaticSlots: Int = 0,
    val snoozeAt: Long? = null,
    val lastShownAt: Long? = null,
    val expiredShownAt: Long? = null,
    val deleteAt: Long? = null,
    val consumedSnoozeAt: Long? = null
)

enum class ReminderKind { MORNING, EVENING, SNOOZE, EXPIRED }

sealed interface ReminderDecision {
    data class Wait(val until: Long) : ReminderDecision
    data class Show(val kind: ReminderKind, val consumedSlots: Int) : ReminderDecision
    data class Skip(val consumedSlots: Int) : ReminderDecision
    data object AwaitRemote : ReminderDecision
}

/** Calendar-day policy; WorkManager wake-up precision does not define business time. */
object ReminderPolicy {
    val snoozeDuration: Duration = Duration.ofHours(1)
    val expirationRetention: Duration = Duration.ofHours(24)

    fun next(scheduledDate: Long, progress: ReminderProgress, now: Long, zone: ZoneId): ReminderDecision {
        val day = Instant.ofEpochMilli(scheduledDate).atZone(zone).toLocalDate()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val morning = day.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant().toEpochMilli()
        val evening = day.atTime(LocalTime.of(18, 0)).atZone(zone).toInstant().toEpochMilli()
        progress.expiredShownAt?.let {
            val deadline = progress.deleteAt ?: return ReminderDecision.AwaitRemote
            return if (now >= deadline) ReminderDecision.AwaitRemote else ReminderDecision.Wait(deadline)
        }
        if (now >= end) return ReminderDecision.Show(ReminderKind.EXPIRED, 3)
        progress.snoozeAt?.takeIf { it != progress.consumedSnoozeAt }?.let { due ->
            if (now < due) return ReminderDecision.Wait(minOf(due, end))
            return ReminderDecision.Show(ReminderKind.SNOOZE, if (now >= evening) 3 else if (now >= morning) 1 else 0)
        }
        if (now >= evening && progress.automaticSlots and 2 == 0) {
            // A just-delivered catch-up notification must not be followed by the evening slot immediately.
            if (progress.lastShownAt != null && now - progress.lastShownAt < snoozeDuration.toMillis()) {
                return ReminderDecision.Skip(3)
            }
            return ReminderDecision.Show(ReminderKind.EVENING, 3)
        }
        if (now >= morning && progress.automaticSlots and 1 == 0) return ReminderDecision.Show(ReminderKind.MORNING, 1)
        return ReminderDecision.Wait(if (now < morning) morning else if (now < evening) evening else end)
    }
}
