package com.ahmetkaragunlu.financeai.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceCalendarTest {
    private val zone = ZoneId.systemDefault()
    private fun midnight(date: String): Instant = LocalDate.parse(date).atStartOfDay(zone).toInstant()

    @Test
    fun `current month covers October first through October last rather than rolling month`() = runTest {
        val calendar = FinanceCalendar(Clock.fixed(midnight("2026-10-05"), zone))

        assertEquals(DateRange(midnight("2026-10-01").toEpochMilli(), midnight("2026-11-01").toEpochMilli()),
            calendar.observeMonth().first())
    }

    @Test
    fun `resume refresh switches month without waiting for the next timer tick`() = runTest {
        val clock = MutableClock(midnight("2026-10-31"), zone)
        val calendar = FinanceCalendar(clock)
        val periods = mutableListOf<DateRange>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            calendar.observeMonth().take(2).toList(periods)
        }
        runCurrent()
        clock.now = midnight("2026-11-01")
        calendar.refresh()
        runCurrent()

        assertEquals(listOf(
            DateRange(midnight("2026-10-01").toEpochMilli(), midnight("2026-11-01").toEpochMilli()),
            DateRange(midnight("2026-11-01").toEpochMilli(), midnight("2026-12-01").toEpochMilli())
        ), periods)
    }

    private class MutableClock(var now: Instant, private val zone: ZoneId) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }
}
