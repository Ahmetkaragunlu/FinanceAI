package com.ahmetkaragunlu.financeai.utils



import java.text.NumberFormat
import java.text.SimpleDateFormat
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

