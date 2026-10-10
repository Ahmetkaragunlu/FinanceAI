package com.ahmetkaragunlu.financeai.core.money

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyAmountsTest {
    @Test
    fun `decimal inputs preserve cents without floating point truncation`() {
        assertEquals(12550L, MoneyAmounts.toMinor(125.50, "TRY"))
        assertEquals(0.3, MoneyAmounts.toMajor(MoneyAmounts.toMinor(0.1 + 0.2, "USD"), "USD"), 0.0)
        assertEquals(125.5, MoneyAmounts.parse("125,50", "EUR")!!, 0.0)
    }

    @Test
    fun `currency scale is not assumed to be two`() {
        assertEquals(125L, MoneyAmounts.toMinor(125.0, "JPY"))
        assertEquals(125125L, MoneyAmounts.toMinor(125.125, "KWD"))
        assertNull(MoneyAmounts.parse("1.01", "JPY"))
    }

    @Test
    fun `invalid non finite overflow and excess precision are rejected`() {
        listOf(
            "",
            "NaN",
            "Infinity",
            "-5",
            "0",
            "1.001",
            "1,2.3",
            "999999999999999999999"
        ).forEach {
            assertNull(it, MoneyAmounts.parse(it, "USD"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            MoneyAmounts.toMinor(
                Double.NaN,
                "USD"
            )
        }
    }

    @Test
    fun `region determines initial currency not the language`() {
        assertEquals("USD", MoneyAmounts.currencyForRegion(Locale.US))
        assertEquals("TRY", MoneyAmounts.currencyForRegion(Locale.forLanguageTag("en-TR")))
        assertNull(MoneyAmounts.currencyForRegion(Locale.ENGLISH))
    }

    @Test
    fun `amount above exact presentation precision is rejected rather than silently changed`() {
        assertNull(MoneyAmounts.parse("9007199254740993", "JPY"))
        assertThrows(IllegalArgumentException::class.java) {
            MoneyAmounts.toMajor(
                9_007_199_254_740_993L,
                "JPY"
            )
        }
    }

    @Test
    fun `legacy decimal conversion rounds half up while new forms reject excess precision`() {
        assertEquals(101L, MoneyAmounts.toMinor(1.005, "USD"))
        assertNull(MoneyAmounts.parse("1.005", "USD"))
    }

    @Test
    fun `financial summary adds minor units instead of accumulating binary decimal noise`() {
        assertEquals(0.3, MoneyAmounts.sum(listOf(0.1, 0.1, 0.1), "USD"), 0.0)
        assertThrows(ArithmeticException::class.java) { MoneyAmounts.readMinor(125.5) }
    }
}
