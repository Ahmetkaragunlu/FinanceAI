package com.ahmetkaragunlu.financeai.core.format

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

fun Double.formatAsCurrency(currencyCode: String = "XXX"): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    formatter.currency = Currency.getInstance(currencyCode)
    formatter.maximumFractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    if (this % 1.0 == 0.0) {
        formatter.maximumFractionDigits = 0
    }
    return formatter.format(this)
}

fun Long.formatAsDate(pattern: String = "dd MMMM yyyy, EEEE"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}

fun Long.formatAsShortDate(pattern: String = "d MMMM"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}
