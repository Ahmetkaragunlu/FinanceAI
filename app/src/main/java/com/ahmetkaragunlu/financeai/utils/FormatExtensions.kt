package com.ahmetkaragunlu.financeai.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

fun Double.formatAsCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    if (this % 1.0 == 0.0) {
        formatter.maximumFractionDigits = 0
    }
    return formatter.format(this)
}

fun getCurrencySymbol(): String {
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).currency?.symbol ?: "₺"
}

fun Long.formatAsDate(pattern: String = "dd MMMM yyyy, EEEE"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}

fun Long.formatAsShortDate(pattern: String = "d MMMM"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}