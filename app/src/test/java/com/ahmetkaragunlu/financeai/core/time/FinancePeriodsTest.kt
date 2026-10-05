package com.ahmetkaragunlu.financeai.core.time

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class FinancePeriodsTest {
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
