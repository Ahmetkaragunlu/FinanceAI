package com.ahmetkaragunlu.financeai.core.format

import com.ahmetkaragunlu.financeai.core.money.UNSPECIFIED_CURRENCY
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import java.util.TimeZone

fun Double.formatAsCurrency(currencyCode: String = UNSPECIFIED_CURRENCY): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    formatter.currency = Currency.getInstance(currencyCode)
    formatter.maximumFractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    if (this % 1.0 == 0.0) {
        formatter.maximumFractionDigits = 0
    }
    return formatter.format(this)
}

fun Long.formatAsDate(pattern: String = "dd MMMM yyyy, EEEE", zone: ZoneId = ZoneId.systemDefault()): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    dateFormat.timeZone = TimeZone.getTimeZone(zone)
    return dateFormat.format(this)
}

fun Long.formatAsShortDate(pattern: String = "d MMMM", zone: ZoneId = ZoneId.systemDefault()): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    dateFormat.timeZone = TimeZone.getTimeZone(zone)
    return dateFormat.format(this)
}
