package com.ahmetkaragunlu.financeai.core.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PercentageFormatterTest {
    @Test fun languagesChangeOnlySymbolPositionAndKeepExistingRoundingOrTruncation() {
        val turkish = Locale.forLanguageTag("tr-TR")
        assertEquals("%50", 50.formatAsPercentage(turkish))
        assertEquals("50%", 50.formatAsPercentage(Locale.US))
        assertEquals("%88", 87.5.formatAsPercentage(turkish))
        assertEquals("88%", 87.5.formatAsPercentage(Locale.US))
        assertEquals("87%", 87.5.toInt().formatAsPercentage(Locale.US))
        assertEquals("%0", 0.formatAsPercentage(turkish))
        assertEquals("120%", 120.formatAsPercentage(Locale.US))
        assertEquals("%-5", (-5).formatAsPercentage(turkish))
    }
}
