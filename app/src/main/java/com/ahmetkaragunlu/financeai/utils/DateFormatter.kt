package com.ahmetkaragunlu.financeai.utils

import android.content.Context
import com.ahmetkaragunlu.financeai.R
import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {

    // Thread-safe formatters
    private val timeFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    private val fullDateFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    }

    private val dateOnlyFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMM", Locale.getDefault())
    }

    // Cache for midnight calculations
    private var cachedMidnightTimestamp: Long = 0L
    private var cachedTodayMidnight: Long = 0L
    private var cachedYesterdayMidnight: Long = 0L

    fun formatRelativeDate(context: Context, timestamp: Long): String {
        val now = System.currentTimeMillis()

        // Update cache if day changed
        if (cachedMidnightTimestamp != getDayStart(now)) {
            updateMidnightCache(now)
        }
        val timePart = timeFormatter.get()!!.format(timestamp)
        return when {
            timestamp >= cachedTodayMidnight -> {
                context.getString(R.string.today) + ", " + timePart
            }
            timestamp >= cachedYesterdayMidnight -> {
                context.getString(R.string.yesterday) + ", " + timePart
            }
            else -> {
                fullDateFormatter.get()!!.format(timestamp)
            }
        }
    }

    fun formatScheduleDate(context: Context, timestamp: Long): String {
        val now = System.currentTimeMillis()
        val todayStart = getDayStart(now)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowStart = calendar.timeInMillis
        val targetStart = getDayStart(timestamp)
        return when {
            targetStart == todayStart -> context.getString(R.string.today)
            targetStart == tomorrowStart -> {
                try {
                    context.getString(R.string.tomorrow)
                } catch (e: Exception) {
                    ""
                }
            }
            else -> dateOnlyFormatter.get()!!.format(timestamp)
        }
    }

    private fun updateMidnightCache(now: Long) {
        cachedMidnightTimestamp = getDayStart(now)
        cachedTodayMidnight = cachedMidnightTimestamp
        cachedYesterdayMidnight = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getDayStart(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getDateRange(dateResId: Int): Pair<Long, Long> {
        return when (dateResId) {
            R.string.today -> getTodayRange()
            R.string.yesterday -> getYesterdayRange()
            R.string.last_week -> getLastWeekRange()
            R.string.last_month -> getLastMonthRange()
            else -> Pair(0L, System.currentTimeMillis())
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = System.currentTimeMillis()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        return Pair(start, end)
    }

    private fun getYesterdayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start: Yesterday 00:00:00
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        // End: Yesterday 23:59:59
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun getLastWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = System.currentTimeMillis()

        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val start = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun getLastMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = System.currentTimeMillis()

        calendar.add(Calendar.MONTH, -1)
        val start = calendar.timeInMillis

        return Pair(start, end)
    }
}


fun Long.formatRelativeDate(context: Context): String {
    return DateFormatter.formatRelativeDate(context, this)
}

fun Long.formatScheduleDate(context: Context): String {
    return DateFormatter.formatScheduleDate(context, this)
}