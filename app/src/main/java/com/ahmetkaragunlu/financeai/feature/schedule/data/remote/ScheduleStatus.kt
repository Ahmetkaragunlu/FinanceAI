package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.core.sync.contract.SyncFields

enum class ScheduleStatus(val wireValue: String) {
    ACTIVE("active"), COMPLETED("completed"), DELETED(SyncFields.DELETED);

    companion object {
        fun fromWire(value: String?): ScheduleStatus? = entries.firstOrNull { it.wireValue == value }
    }
}
