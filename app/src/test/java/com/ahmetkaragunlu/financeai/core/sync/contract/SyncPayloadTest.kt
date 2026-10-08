package com.ahmetkaragunlu.financeai.core.sync.contract

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPayloadTest {
    @Test fun `large long monetary payload is not narrowed to double`() {
        val amount = 9_007_199_254_740_993L
        val encoded = SyncPayload.encode(mapOf("amountMinor" to amount))
        assertEquals(amount, SyncPayload.decode(encoded)["amountMinor"])
    }
}
