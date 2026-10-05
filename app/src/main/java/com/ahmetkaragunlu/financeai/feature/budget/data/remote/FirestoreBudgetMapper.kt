package com.ahmetkaragunlu.financeai.feature.budget.data.remote

import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget

internal fun Budget.toFirebaseMap(): Map<String, Any?> = mapOf(
    "budgetType" to budgetType.name,
    "category" to category?.name,
    "amountMinor" to MoneyAmounts.toMinor(amount, currencyCode),
    "currencyCode" to currencyCode,
    "amount" to amount,
    "limitPercentage" to limitPercentage
)
