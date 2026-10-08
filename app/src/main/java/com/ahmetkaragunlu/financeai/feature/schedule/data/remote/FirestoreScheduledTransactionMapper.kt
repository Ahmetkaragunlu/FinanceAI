package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.ScheduleFields
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction

internal fun ScheduledTransaction.toFirebaseMap(): Map<String, Any?> = mapOf(
    FinancialFields.AMOUNT_MINOR to MoneyAmounts.toMinor(amount, currencyCode),
    FinancialFields.CURRENCY_CODE to currencyCode,
    FinancialFields.LEGACY_AMOUNT to amount,
    ScheduleFields.TYPE to type.name,
    FinancialFields.CATEGORY to category.name,
    FinancialFields.NOTE to note,
    ScheduleFields.DATE to scheduledDate,
    ScheduleFields.EXPIRATION_SENT to expirationNotificationSent,
    ScheduleFields.NOTIFICATION_SENT to notificationSent,
    FinancialFields.LOCATION_FULL to locationFull,
    FinancialFields.LOCATION_SHORT to locationShort,
    FinancialFields.LATITUDE to latitude,
    FinancialFields.LONGITUDE to longitude
)
