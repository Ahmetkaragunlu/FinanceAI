package com.ahmetkaragunlu.financeai.utils



import android.content.Context
import com.ahmetkaragunlu.financeai.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun Double.formatAsCurrency(): String {
    val systemLocale = Locale.getDefault()
    val formatter = NumberFormat.getCurrencyInstance(systemLocale)
    return formatter.format(this)
}

fun getCurrencySymbol(): String {
    val systemLocale = Locale.getDefault()
    return NumberFormat.getCurrencyInstance(systemLocale).currency?.symbol ?: "₺"
}


fun Long.formatAsDate(pattern: String = "dd MMMM yyyy, EEEE"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}

fun Long.formatAsShortDate(pattern: String = "d MMMM"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}


fun Long.formatScheduleDate(context: Context): String {
    val currentCalendar = Calendar.getInstance()
    val targetCalendar = Calendar.getInstance().apply { timeInMillis = this@formatScheduleDate }

    val isSameDay = currentCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
            currentCalendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)

    return if (isSameDay) {
        context.getString(R.string.today)
    } else {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        dateFormat.format(this)
    }
}