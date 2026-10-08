package com.ahmetkaragunlu.financeai.core.format

import android.content.Context
import com.ahmetkaragunlu.financeai.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    fun formatScheduleDate(context: Context, timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        return when (date) {
            LocalDate.now(zone) -> context.getString(R.string.today)
            LocalDate.now(zone).plusDays(1) -> context.getString(R.string.tomorrow)
            else -> date.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))
        }
    }

}
fun Long.formatRelativeDate(context: Context): String = DateFormatter.formatRelativeDate(context, this)
fun Long.formatScheduleDate(context: Context, zone: ZoneId = ZoneId.systemDefault()): String = DateFormatter.formatScheduleDate(context, this, zone)
