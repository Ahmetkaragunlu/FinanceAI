package com.ahmetkaragunlu.financeai.feature.transaction.data.remote

import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

internal fun Transaction.toFirebaseMap(): Map<String, Any?> = mapOf(
    "amountMinor" to MoneyAmounts.toMinor(amount, currencyCode),
    "currencyCode" to currencyCode,
    "amount" to amount,
    "transaction" to transaction.name,
    "note" to note,
    "date" to date,
    "category" to category.name,
    "locationFull" to locationFull,
    "locationShort" to locationShort,
    "latitude" to latitude,
    "longitude" to longitude
)
