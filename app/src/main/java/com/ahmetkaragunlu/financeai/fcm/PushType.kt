package com.ahmetkaragunlu.financeai.fcm

/** Push protocol values; persisted receipts and WorkManager input retain these exact strings. */
enum class PushType(val wireValue: String) {
    SCHEDULE_STATE_CHANGED("SCHEDULE_STATE_CHANGED"),
    SCHEDULED_REMINDER("SCHEDULED_REMINDER"),
    CANCEL_NOTIFICATION("CANCEL_NOTIFICATION"),
    DISMISS_NOTIFICATION("DISMISS_NOTIFICATION"),
    RESCHEDULE_NOTIFICATION("RESCHEDULE_NOTIFICATION");

    companion object {
        fun fromWire(value: String): PushType? = entries.firstOrNull { it.wireValue == value }
    }
}
