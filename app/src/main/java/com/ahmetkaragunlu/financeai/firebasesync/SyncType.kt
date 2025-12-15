package com.ahmetkaragunlu.financeai.firebasesync

 enum class SyncType(val collectionName: String) {
    TRANSACTION("transactions"),
    SCHEDULED("scheduled_transactions"),
    BUDGET("budgets"),
    AI_MESSAGE("ai_messages")
}
