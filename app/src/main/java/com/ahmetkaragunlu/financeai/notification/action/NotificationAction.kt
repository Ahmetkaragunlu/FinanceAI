package com.ahmetkaragunlu.financeai.notification.action

/** Internal behaviour with canonical Android identities for newly created PendingIntents. */
enum class NotificationAction(val intentAction: String) {
    CONFIRM(NotificationActions.ACTION_CONFIRM),
    SNOOZE(NotificationActions.ACTION_SNOOZE),
    DISMISS(NotificationActions.ACTION_DISMISS)
}
