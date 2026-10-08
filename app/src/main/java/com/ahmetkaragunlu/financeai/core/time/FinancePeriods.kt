package com.ahmetkaragunlu.financeai.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class DateRange(val start: Long, val endExclusive: Long)

object FinancePeriods {
    /** Preserve rolling history filters and their excluded upper bound. */
    fun filter(filter: DateFilter, clock: Clock): DateRange {
        val now = ZonedDateTime.now(clock)
        return when (filter) {
            DateFilter.TODAY -> DateRange(day(now.toLocalDate(), clock.zone).start, clock.millis() + 1)
            DateFilter.YESTERDAY -> day(now.toLocalDate().minusDays(1), clock.zone)
            DateFilter.LAST_WEEK -> DateRange(now.minusDays(7).toInstant().toEpochMilli(), clock.millis() + 1)
            DateFilter.LAST_MONTH -> DateRange(now.minusMonths(1).toInstant().toEpochMilli(), clock.millis() + 1)
            DateFilter.ALL -> DateRange(0, Long.MAX_VALUE)
        }
    }

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
    fun toPicker(millis: Long, zone: ZoneId): Long = Instant.ofEpochMilli(millis)
        .atZone(zone).toLocalDate().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}
