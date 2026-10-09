package com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder

import com.ahmetkaragunlu.financeai.feature.schedule.testing.fixtureTime
import com.ahmetkaragunlu.financeai.feature.schedule.testing.scheduleContractFixture
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderContractFixtureTest {
    @Test fun calendarSlotsDstAndConsumedBitsMatchExpectedDeviceDecisionsWithoutCopyingServerTick() {
        val fixture = scheduleContractFixture()
        for (dayValue in fixture.getAsJsonArray("days")) {
            val day = dayValue.asJsonObject
            val zone = ZoneId.of(day["zone"].asString)
            for (checkValue in fixture.getAsJsonArray("checks")) {
                val check = checkValue.asJsonObject
                val now = day.fixtureTime(check["time"].asString) + check["offsetMillis"].asLong
                val expected = if (check["androidKind"].asString == "WAIT")
                    ReminderDecision.Wait(day.fixtureTime(check["androidUntil"].asString))
                else ReminderDecision.Show(ReminderKind.valueOf(check["androidKind"].asString), check["slots"].asInt)
                assertEquals("${day["zone"]}/${check["time"]}", expected,
                    ReminderPolicy.next(day.fixtureTime("date"), ReminderProgress(automaticSlots = check["progressSlots"].asInt), now, zone))
            }
        }
    }

    @Test fun snoozeAndAcceptedRetentionKeepOneSharedDeadlineWithoutLocalDeletionAuthority() {
        val fixture = scheduleContractFixture()
        assertEquals(fixture["snoozeMillis"].asLong, ReminderPolicy.snoozeDuration.toMillis())
        assertEquals(fixture["retentionMillis"].asLong, ReminderPolicy.expirationRetention.toMillis())
        val day = fixture.getAsJsonArray("days")[0].asJsonObject
        val snooze = fixture.getAsJsonObject("snooze")
        assertEquals(ReminderDecision.Show(ReminderKind.SNOOZE, snooze["slots"].asInt),
            ReminderPolicy.next(day.fixtureTime("date"), ReminderProgress(snoozeAt = snooze.fixtureTime("due")),
                snooze.fixtureTime("reconnected"), ZoneId.of(day["zone"].asString)))
        val expiration = fixture.getAsJsonObject("expiration")
        val deadline = expiration.fixtureTime("deadline")
        val progress = ReminderProgress(expiredShownAt = expiration.fixtureTime("shown"), deleteAt = deadline)
        assertEquals(ReminderDecision.Wait(deadline), ReminderPolicy.next(day.fixtureTime("date"), progress, deadline - 1, ZoneId.of(day["zone"].asString)))
        assertEquals(ReminderDecision.AwaitRemote, ReminderPolicy.next(day.fixtureTime("date"), progress, deadline, ZoneId.of(day["zone"].asString)))
    }
}
