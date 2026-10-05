package com.ahmetkaragunlu.financeai.fcm

import org.junit.Assert.*
import org.junit.Test

class PushPayloadTest {
    private val values = mapOf("userId" to "A", "transactionId" to "p1", "type" to "SCHEDULE_STATE_CHANGED", "eventId" to "p1_7")
    @Test fun newPayloadUsesStableEventIdentity() {
        assertEquals(PushPayload("A", "p1", "p1_7", "SCHEDULE_STATE_CHANGED"), PushPayload.parse(values, "transport"))
    }
    @Test fun missingOwnerAndUnknownTypeAreRejected() {
        assertNull(PushPayload.parse(values - "userId", "transport"))
        assertNull(PushPayload.parse(values + ("type" to "EXECUTE_COMPLETION"), "transport"))
    }
    @Test fun legacyMessagesRequireTransportIdentity() {
        assertNotNull(PushPayload.parse(values - "eventId", "transport"))
        assertNull(PushPayload.parse(values - "eventId", null))
    }
    @Test fun pathInjectionIsRejected() {
        assertNull(PushPayload.parse(values + ("transactionId" to "../B"), "transport"))
    }
}
