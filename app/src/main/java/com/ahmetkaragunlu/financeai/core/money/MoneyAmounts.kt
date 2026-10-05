package com.ahmetkaragunlu.financeai.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale

/** Decimal conversion belongs at the form/storage boundary, not in UI widgets. */
object MoneyAmounts {
    fun currencyForRegion(locale: Locale): String? = runCatching {
        Currency.getInstance(locale).takeIf { it.defaultFractionDigits >= 0 }?.currencyCode
    }.getOrNull()

    fun scale(currencyCode: String): Int = Currency.getInstance(currencyCode)
        .defaultFractionDigits.coerceAtLeast(0)

    fun toMinor(amount: Double, currencyCode: String): Long {
        require(amount.isFinite() && amount >= 0) { "Invalid amount" }
        return BigDecimal.valueOf(amount).movePointRight(scale(currencyCode))
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    fun sum(amounts: Iterable<Double>, currencyCode: String): Double =
        toMajor(amounts.fold(0L) { total, amount -> Math.addExact(total, toMinor(amount, currencyCode)) }, currencyCode)

    fun readMinor(value: Number): Long = BigDecimal(value.toString()).longValueExact()

    fun toMajor(minor: Long, currencyCode: String): Double {
        val value = BigDecimal.valueOf(minor, scale(currencyCode)).toDouble()
        require(BigDecimal.valueOf(value).movePointRight(scale(currencyCode))
            .setScale(0, RoundingMode.HALF_UP).longValueExact() == minor) { "Amount exceeds supported precision" }
        return value
    }

    fun parse(text: String, currencyCode: String): Double? = try {
        val decimal = text.trim().replace(',', '.').toBigDecimal()
        require(decimal.signum() > 0)
        val minor = decimal.movePointRight(scale(currencyCode))
            .setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        require(minor > 0)
        toMajor(minor, currencyCode)
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}
