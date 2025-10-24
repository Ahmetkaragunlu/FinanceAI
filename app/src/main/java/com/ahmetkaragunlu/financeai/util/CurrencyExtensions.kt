package com.ahmetkaragunlu.financeai.util


import java.text.NumberFormat
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