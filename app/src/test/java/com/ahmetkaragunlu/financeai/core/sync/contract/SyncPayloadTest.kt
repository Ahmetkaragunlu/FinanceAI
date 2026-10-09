package com.ahmetkaragunlu.financeai.core.sync.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPayloadTest {
    @Test fun `large long monetary payload is not narrowed to double`() {
        val amount = 9_007_199_254_740_993L
        val encoded = SyncPayload.encode(mapOf("amountMinor" to amount))
        assertEquals(amount, SyncPayload.decode(encoded)["amountMinor"])
    }

    @Test fun `integral and fractional numbers retain their deliberate wire normalization`() {
        val decoded = SyncPayload.decode("{\"whole\":1.0,\"fraction\":1.25,\"text\":\"1\",\"flag\":false,\"empty\":null}")
        assertEquals(1L, decoded["whole"])
        assertEquals(1.25, decoded["fraction"])
        assertEquals("1", decoded["text"])
        assertEquals(false, decoded["flag"])
        assertTrue(decoded.containsKey("empty"))
        assertEquals(null, decoded["empty"])
    }

    @Test fun `equivalence ignores key order and integral notation without equating missing or different fields`() {
        assertTrue(SyncPayload.equivalent("{\"amount\":1,\"note\":null}", "{\"note\":null,\"amount\":1.0}"))
        assertFalse(SyncPayload.equivalent("{\"note\":null}", "{}"))
        assertFalse(SyncPayload.equivalent("{\"amount\":1}", "{\"amount\":1.25}"))
        assertFalse(SyncPayload.equivalent("{\"amount\":1}", "{\"amount\":\"1\"}"))
        assertTrue(SyncPayload.equivalent(null, null))
        assertFalse(SyncPayload.equivalent(null, "{}"))
        assertEquals("{\"a\":null,\"z\":true}", SyncPayload.encode(linkedMapOf("z" to true, "a" to null)))
    }

    @Test fun `long boundaries round trip and overflow is rejected rather than narrowed`() {
        assertEquals(mapOf("min" to Long.MIN_VALUE, "max" to Long.MAX_VALUE),
            SyncPayload.decode(SyncPayload.encode(mapOf("min" to Long.MIN_VALUE, "max" to Long.MAX_VALUE))))
        assertThrows(ArithmeticException::class.java) { SyncPayload.decode("{\"amountMinor\":9223372036854775808}") }
        assertThrows(ArithmeticException::class.java) { SyncPayload.decode("{\"amountMinor\":-9223372036854775809}") }
    }
}
