package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction

internal fun ScheduledTransaction.toFirebaseMap(): Map<String, Any?> = mapOf(
    "amountMinor" to MoneyAmounts.toMinor(amount, currencyCode),
    "currencyCode" to currencyCode,
    "amount" to amount,
    "type" to type.name,
    "category" to category.name,
    "note" to note,
    "scheduledDate" to scheduledDate,
    "expirationNotificationSent" to expirationNotificationSent,
    "notificationSent" to notificationSent,
    "locationFull" to locationFull,
    "locationShort" to locationShort,
    "latitude" to latitude,
    "longitude" to longitude
)
