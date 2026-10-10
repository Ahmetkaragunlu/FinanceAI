package com.ahmetkaragunlu.financeai.notification.action

/** Stable Android action identities; keep values compatible with persisted PendingIntents. */
object NotificationActions {
    const val ACTION_CONFIRM = "com.ahmetkaragunlu.financeai.ACTION_CONFIRM"
    // Old immutable PendingIntents use CANCEL for snooze, not deletion/cancellation of the plan.
    const val ACTION_CANCEL = "com.ahmetkaragunlu.financeai.ACTION_CANCEL"
    const val ACTION_SNOOZE = "com.ahmetkaragunlu.financeai.ACTION_SNOOZE"
    const val ACTION_DISMISS = "com.ahmetkaragunlu.financeai.ACTION_DISMISS"

    fun parse(value: String?): NotificationAction? = when (value) {
        ACTION_CONFIRM -> NotificationAction.CONFIRM
        ACTION_CANCEL, ACTION_SNOOZE -> NotificationAction.SNOOZE
        ACTION_DISMISS -> NotificationAction.DISMISS
        else -> null
    }

    fun isSupported(value: String): Boolean = parse(value) != null
}
