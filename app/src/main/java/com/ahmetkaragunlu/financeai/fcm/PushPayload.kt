package com.ahmetkaragunlu.financeai.fcm

import com.ahmetkaragunlu.financeai.core.sync.contract.SyncFields

data class PushPayload(val ownerId: String, val remoteId: String, val eventId: String, val type: String) {
    companion object {
        private val supported = setOf("SCHEDULE_STATE_CHANGED", "SCHEDULED_REMINDER", "CANCEL_NOTIFICATION",
            "DISMISS_NOTIFICATION", "RESCHEDULE_NOTIFICATION")
        fun parse(data: Map<String, String>, messageId: String?): PushPayload? {
            val owner = data[SyncFields.USER_ID]?.takeIf { it.isNotBlank() && it.length <= 128 } ?: return null
            val record = data[PushFields.TRANSACTION_ID]
                ?.takeIf { it.isNotBlank() && '/' !in it && it.length <= 512 } ?: return null
            val type = data[PushFields.TYPE]?.takeIf { it in supported } ?: return null
            val event = data[PushFields.EVENT_ID]?.takeIf { it.isNotBlank() }
                ?: messageId?.takeIf { it.isNotBlank() } ?: return null
            if (event.length > 1024) return null
            return PushPayload(owner, record, event, type)
        }
    }
}
