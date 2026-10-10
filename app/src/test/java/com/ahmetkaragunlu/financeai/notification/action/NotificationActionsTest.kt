package com.ahmetkaragunlu.financeai.notification.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationActionsTest {
    @Test
    fun persistedAndroidActionsMapToTheSameBehavioursIncludingLegacyCancel() {
        val actions = listOf(
            "com.ahmetkaragunlu.financeai.ACTION_CONFIRM" to NotificationAction.CONFIRM,
            "com.ahmetkaragunlu.financeai.ACTION_CANCEL" to NotificationAction.SNOOZE,
            "com.ahmetkaragunlu.financeai.ACTION_SNOOZE" to NotificationAction.SNOOZE,
            "com.ahmetkaragunlu.financeai.ACTION_DISMISS" to NotificationAction.DISMISS,
        )
        for ((raw, expected) in actions) {
            assertEquals(expected, NotificationActions.parse(raw))
            assertTrue(NotificationActions.isSupported(raw))
        }
    }

    @Test
    fun newPendingIntentActionsRetainTheirCanonicalIdentitiesAndRoundTrip() {
        val actions = listOf(
            NotificationAction.CONFIRM to "com.ahmetkaragunlu.financeai.ACTION_CONFIRM",
            NotificationAction.SNOOZE to "com.ahmetkaragunlu.financeai.ACTION_SNOOZE",
            NotificationAction.DISMISS to "com.ahmetkaragunlu.financeai.ACTION_DISMISS",
        )
        for ((action, raw) in actions) {
            assertEquals(raw, action.intentAction)
            assertEquals(action, NotificationActions.parse(action.intentAction))
        }
    }

    @Test
    fun missingUnknownAndNonExactActionsRemainRejected() {
        assertNull(NotificationActions.parse(null))
        for (raw in listOf(
            "", "CONFIRM", "unknown", "com.ahmetkaragunlu.financeai.action_confirm",
            "com.ahmetkaragunlu.financeai.ACTION_CONFIRM ",
        )) {
            assertNull(NotificationActions.parse(raw))
            assertFalse(NotificationActions.isSupported(raw))
        }
    }
}
