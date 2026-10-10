package com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder

enum class ScheduleCommandType(val wireValue: String) {
    COMPLETE("complete"), SNOOZE("snooze"), EXPIRATION_SHOWN("expiration_shown");

    companion object {
        fun fromWire(value: String?): ScheduleCommandType? =
            entries.firstOrNull { it.wireValue == value }
    }
}
