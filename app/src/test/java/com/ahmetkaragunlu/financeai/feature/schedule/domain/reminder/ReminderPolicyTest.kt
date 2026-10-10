package com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderPolicyTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val day = LocalDate.of(2026, 10, 5)
    private fun at(hour: Int) = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private val date = at(0)

    @Test
    fun firstAutomaticReminderIsAtNineInAccountZone() {
        assertEquals(
            ReminderDecision.Wait(at(9)),
            ReminderPolicy.next(date, ReminderProgress(), at(8), zone)
        )
        assertEquals(
            ReminderDecision.Show(ReminderKind.MORNING, 1),
            ReminderPolicy.next(date, ReminderProgress(), at(9), zone)
        )
    }

    @Test
    fun eveningCatchUpDoesNotReplayMorning() {
        assertEquals(
            ReminderDecision.Show(ReminderKind.EVENING, 3),
            ReminderPolicy.next(date, ReminderProgress(), at(19), zone)
        )
    }

    @Test
    fun consumedMorningWaitsForEvening() {
        assertEquals(
            ReminderDecision.Wait(at(18)),
            ReminderPolicy.next(date, ReminderProgress(automaticSlots = 1), at(10), zone)
        )
    }

    @Test
    fun overlappingSnoozeConsumesEveningOnce() {
        val progress = ReminderProgress(1, snoozeAt = at(18))
        assertEquals(
            ReminderDecision.Show(ReminderKind.SNOOZE, 3),
            ReminderPolicy.next(date, progress, at(18), zone)
        )
        val consumed =
            progress.copy(automaticSlots = 3, consumedSnoozeAt = at(18), lastShownAt = at(18))
        assertEquals(
            ReminderDecision.Wait(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()),
            ReminderPolicy.next(date, consumed, at(18), zone)
        )
    }

    @Test
    fun snoozeDoesNotBecomeAnHourlyAutomaticLoop() {
        val progress =
            ReminderProgress(1, snoozeAt = at(11), consumedSnoozeAt = at(11), lastShownAt = at(11))
        assertEquals(
            ReminderDecision.Wait(at(18)),
            ReminderPolicy.next(date, progress, at(12), zone)
        )
    }

    @Test
    fun localExpirationReceiptDoesNotStartLocalDeletion() {
        val nextDay = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(
            ReminderDecision.AwaitRemote,
            ReminderPolicy.next(
                date,
                ReminderProgress(expiredShownAt = nextDay),
                nextDay + 86_400_000,
                zone
            )
        )
    }

    @Test
    fun sharedRetentionDeadlineNeverDeletesLocallyWithoutServerState() {
        val shown = date + 86_400_000
        val deadline = shown + 86_400_000
        val progress = ReminderProgress(expiredShownAt = shown, deleteAt = deadline)
        assertEquals(
            ReminderDecision.Wait(deadline),
            ReminderPolicy.next(date, progress, shown, zone)
        )
        assertEquals(
            ReminderDecision.AwaitRemote,
            ReminderPolicy.next(date, progress, deadline, zone)
        )
    }

    @Test
    fun springDstUsesCalendarMidnight() {
        val berlin = ZoneId.of("Europe/Berlin")
        val midnight = LocalDate.of(2026, 3, 29).atStartOfDay(berlin).toInstant().toEpochMilli()
        val atEvening =
            LocalDate.of(2026, 3, 29).atTime(19, 0).atZone(berlin).toInstant().toEpochMilli()
        assertEquals(
            ReminderDecision.Wait(midnight + 23 * 3_600_000),
            ReminderPolicy.next(midnight, ReminderProgress(3), atEvening, berlin)
        )
    }
}
