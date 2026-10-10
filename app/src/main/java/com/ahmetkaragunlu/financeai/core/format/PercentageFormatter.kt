package com.ahmetkaragunlu.financeai.core.format

import java.util.Locale

/** Values are percentages (50), not ratios (0.5); callers retain their own financial meaning. */
fun Int.formatAsPercentage(locale: Locale): String =
    percentageText(String.format(locale, "%d", this), locale)

/** Keeps Home's existing zero-decimal rounding, without changing Budget's integer truncation. */
fun Double.formatAsPercentage(locale: Locale): String =
    percentageText(String.format(locale, "%.0f", this), locale)

private fun percentageText(number: String, locale: Locale): String =
    if (locale.language == "tr") "%$number" else "$number%"
