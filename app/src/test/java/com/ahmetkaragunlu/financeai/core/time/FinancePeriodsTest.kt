package com.ahmetkaragunlu.financeai.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancePeriodsTest {
    @Test fun `history today ends just after now and yesterday ends at local midnight`() {
        val zone = ZoneId.of("Europe/Istanbul")
        val now = Instant.parse("2026-10-08T12:00:00Z")
        val clock = Clock.fixed(now, zone)
        assertEquals(DateRange(Instant.parse("2026-10-07T21:00:00Z").toEpochMilli(), now.toEpochMilli() + 1),
            FinancePeriods.filter(DateFilter.TODAY, clock))
        assertEquals(DateRange(Instant.parse("2026-10-06T21:00:00Z").toEpochMilli(), Instant.parse("2026-10-07T21:00:00Z").toEpochMilli()),
            FinancePeriods.filter(DateFilter.YESTERDAY, clock))
        assertEquals(DateRange(0, Long.MAX_VALUE), FinancePeriods.filter(DateFilter.ALL, clock))
    }

    @Test fun `history last month remains rolling and clamps to previous month last day`() {
        val clock = Clock.fixed(Instant.parse("2026-03-31T12:30:00Z"), ZoneOffset.UTC)
        assertEquals(DateRange(Instant.parse("2026-02-28T12:30:00Z").toEpochMilli(), clock.millis() + 1),
            FinancePeriods.filter(DateFilter.LAST_MONTH, clock))
    }

    @Test fun `history last week follows calendar days across daylight saving change`() {
        val zone = ZoneId.of("Europe/Berlin")
        val clock = Clock.fixed(ZonedDateTime.of(2026, 3, 31, 12, 0, 0, 0, zone).toInstant(), zone)
        val range = FinancePeriods.filter(DateFilter.LAST_WEEK, clock)
        assertEquals(ZonedDateTime.of(2026, 3, 24, 12, 0, 0, 0, zone).toInstant().toEpochMilli(), range.start)
        assertEquals(clock.millis() + 1, range.endExclusive)
    }
    @Test fun `picker roundtrip keeps account day across east and west time zones`() {
        val picker = Instant.parse("2026-10-05T00:00:00Z").toEpochMilli()
        for (name in listOf("Europe/Istanbul", "Asia/Tokyo", "America/Los_Angeles")) {
            val zone = ZoneId.of(name)
            assertEquals(picker, FinancePeriods.toPicker(FinancePeriods.fromPicker(picker, zone), zone))
        }
    }
    @Test fun `month has exclusive next month boundary across year change`() {
        val clock = Clock.fixed(Instant.parse("2026-12-31T20:00:00Z"), ZoneId.of("Europe/Istanbul"))
        val month = FinancePeriods.month(clock)
        assertEquals(Instant.parse("2026-11-30T21:00:00Z").toEpochMilli(), month.start)
        assertEquals(Instant.parse("2026-12-31T21:00:00Z").toEpochMilli(), month.endExclusive)
    }
    @Test fun `calendar day respects daylight savings rather than fixed 24 hours`() {
        val day = FinancePeriods.day(LocalDate.of(2026, 3, 29), ZoneId.of("Europe/Berlin"))
        assertEquals(23 * 60 * 60 * 1000L, day.endExclusive - day.start)
    }
    @Test fun `UTC picker date is preserved west of UTC`() {
        val selected = Instant.parse("2026-10-05T00:00:00Z").toEpochMilli()
        val result = FinancePeriods.fromPicker(selected, ZoneId.of("America/Los_Angeles"))
        assertEquals(LocalDate.of(2026, 10, 5), Instant.ofEpochMilli(result).atZone(ZoneId.of("America/Los_Angeles")).toLocalDate())
    }
}
