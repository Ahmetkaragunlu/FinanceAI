package com.ahmetkaragunlu.financeai.notification.action

/** Stable Android action identities; keep values compatible with persisted PendingIntents. */
object NotificationActions {
    const val ACTION_CONFIRM = "com.ahmetkaragunlu.financeai.ACTION_CONFIRM"
    // Old immutable PendingIntents use CANCEL for snooze, not deletion/cancellation of the plan.
    const val ACTION_CANCEL = "com.ahmetkaragunlu.financeai.ACTION_CANCEL"
    const val ACTION_SNOOZE = "com.ahmetkaragunlu.financeai.ACTION_SNOOZE"
    const val ACTION_DISMISS = "com.ahmetkaragunlu.financeai.ACTION_DISMISS"

    fun isSupported(value: String): Boolean = when (value) {
        ACTION_CONFIRM, ACTION_CANCEL, ACTION_SNOOZE, ACTION_DISMISS -> true
        else -> false
    }
}
