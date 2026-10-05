package com.ahmetkaragunlu.financeai.core.format

import android.content.Context
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    fun formatRelativeDate(context: Context, timestamp: Long): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(timestamp).atZone(zone)
        val today = LocalDate.now(zone)
        val time = date.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
        return when (date.toLocalDate()) {
            today -> context.getString(R.string.today) + ", " + time
            today.minusDays(1) -> context.getString(R.string.yesterday) + ", " + time
            else -> date.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.getDefault()))
        }
    }

    fun formatScheduleDate(context: Context, timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return when (date) {
            LocalDate.now() -> context.getString(R.string.today)
            LocalDate.now().plusDays(1) -> context.getString(R.string.tomorrow)
            else -> date.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))
        }
    }

    /** All DAO ranges have an excluded upper bound. Existing rolling filter semantics remain. */
    fun getDateRange(dateResId: Int, clock: Clock = Clock.systemDefaultZone()): Pair<Long, Long> {
        val now = ZonedDateTime.now(clock)
        return when (dateResId) {
            R.string.today -> FinancePeriods.day(now.toLocalDate(), clock.zone).let { it.start to clock.millis() + 1 }
            R.string.yesterday -> FinancePeriods.day(now.toLocalDate().minusDays(1), clock.zone).let { it.start to it.endExclusive }
            R.string.last_week -> now.minusDays(7).toInstant().toEpochMilli() to clock.millis() + 1
            R.string.last_month -> now.minusMonths(1).toInstant().toEpochMilli() to clock.millis() + 1
            else -> 0L to Long.MAX_VALUE
        }
    }

    fun getCurrentMonthRange(): Pair<Long, Long> = FinancePeriods.month(Clock.systemDefaultZone()).let { it.start to it.endExclusive }
}
fun Long.formatRelativeDate(context: Context): String = DateFormatter.formatRelativeDate(context, this)
fun Long.formatScheduleDate(context: Context): String = DateFormatter.formatScheduleDate(context, this)
