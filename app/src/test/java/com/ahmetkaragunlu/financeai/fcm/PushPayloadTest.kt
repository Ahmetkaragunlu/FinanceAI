package com.ahmetkaragunlu.financeai.fcm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    @Test fun exactOwnerRecordAndEventLengthLimitsAreAcceptedWhileNextCharacterIsRejected() {
        val maximum = values + mapOf("userId" to "u".repeat(128), "transactionId" to "p".repeat(512), "eventId" to "e".repeat(1024))
        assertNotNull(PushPayload.parse(maximum, null))
        for ((field, length) in listOf("userId" to 129, "transactionId" to 513, "eventId" to 1025))
            assertNull(PushPayload.parse(maximum + (field to "x".repeat(length)), null))
        assertNotNull(PushPayload.parse(values - "eventId", "m".repeat(1024)))
        assertNull(PushPayload.parse(values - "eventId", "m".repeat(1025)))
        assertNull(PushPayload.parse(values + ("eventId" to "e".repeat(1025)), "valid-transport"))
    }
    @Test fun allFivePreservedWireTypesKeepTheirIdentityWithoutExecutingFinancialCommands() {
        listOf("SCHEDULE_STATE_CHANGED", "SCHEDULED_REMINDER", "CANCEL_NOTIFICATION", "DISMISS_NOTIFICATION", "RESCHEDULE_NOTIFICATION").forEach {
            assertEquals(it, PushPayload.parse(values + ("type" to it), null)?.type)
        }
        assertNull(PushPayload.parse(values + ("type" to "complete"), null))
    }
}
