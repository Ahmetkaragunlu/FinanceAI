package com.ahmetkaragunlu.financeai.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DateRange(val start: Long, val endExclusive: Long)

object FinancePeriods {
    fun month(clock: Clock): DateRange {
        val first = LocalDate.now(clock).withDayOfMonth(1)
        return DateRange(first.atStartOfDay(clock.zone).toInstant().toEpochMilli(),
            first.plusMonths(1).atStartOfDay(clock.zone).toInstant().toEpochMilli())
    }
    fun day(date: LocalDate, zone: ZoneId): DateRange = DateRange(
        date.atStartOfDay(zone).toInstant().toEpochMilli(),
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())

    /** Material date picker millis encode a UTC calendar date, not a local midnight instant. */
    fun fromPicker(millis: Long, zone: ZoneId): Long = Instant.ofEpochMilli(millis)
        .atZone(ZoneId.of("UTC")).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
}
