package com.ahmetkaragunlu.financeai.fcm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PushPayloadTest {
    private val values = mapOf(
        "userId" to "A",
        "transactionId" to "p1",
        "type" to "SCHEDULE_STATE_CHANGED",
        "eventId" to "p1_7"
    )

    @Test
    fun newPayloadUsesStableEventIdentity() {
        assertEquals(
            PushPayload("A", "p1", "p1_7", PushType.SCHEDULE_STATE_CHANGED),
            PushPayload.parse(values, "transport"),
        )
    }

    @Test
    fun missingOwnerAndUnknownTypeAreRejected() {
        assertNull(PushPayload.parse(values - "userId", "transport"))
        assertNull(PushPayload.parse(values - "type", "transport"))
        for (unsupported in listOf(
            "EXECUTE_COMPLETION",
            "",
            "schedule_state_changed",
            "SCHEDULE_STATE_CHANGED "
        )) {
            assertNull(PushPayload.parse(values + ("type" to unsupported), "transport"))
        }
    }

    @Test
    fun legacyMessagesRequireTransportIdentity() {
        assertNotNull(PushPayload.parse(values - "eventId", "transport"))
        assertNull(PushPayload.parse(values - "eventId", null))
    }

    @Test
    fun pathInjectionIsRejected() {
        assertNull(PushPayload.parse(values + ("transactionId" to "../B"), "transport"))
    }

    @Test
    fun exactOwnerRecordAndEventLengthLimitsAreAcceptedWhileNextCharacterIsRejected() {
        val maximum = values + mapOf(
            "userId" to "u".repeat(128),
            "transactionId" to "p".repeat(512),
            "eventId" to "e".repeat(1024)
        )
        assertNotNull(PushPayload.parse(maximum, null))
        for ((field, length) in listOf("userId" to 129, "transactionId" to 513, "eventId" to 1025))
            assertNull(PushPayload.parse(maximum + (field to "x".repeat(length)), null))
        assertNotNull(PushPayload.parse(values - "eventId", "m".repeat(1024)))
        assertNull(PushPayload.parse(values - "eventId", "m".repeat(1025)))
        assertNull(PushPayload.parse(values + ("eventId" to "e".repeat(1025)), "valid-transport"))
    }

    @Test
    fun allFivePreservedWireTypesKeepTheirIdentityWithoutExecutingFinancialCommands() {
        val wireTypes = listOf(
            "SCHEDULE_STATE_CHANGED" to PushType.SCHEDULE_STATE_CHANGED,
            "SCHEDULED_REMINDER" to PushType.SCHEDULED_REMINDER,
            "CANCEL_NOTIFICATION" to PushType.CANCEL_NOTIFICATION,
            "DISMISS_NOTIFICATION" to PushType.DISMISS_NOTIFICATION,
            "RESCHEDULE_NOTIFICATION" to PushType.RESCHEDULE_NOTIFICATION,
        )
        for ((wire, expected) in wireTypes) {
            val payload = checkNotNull(PushPayload.parse(values + ("type" to wire), null))
            assertEquals(expected, payload.type)
            assertEquals(wire, payload.type.wireValue)
        }
        assertNull(PushPayload.parse(values + ("type" to "complete"), null))
    }
}
