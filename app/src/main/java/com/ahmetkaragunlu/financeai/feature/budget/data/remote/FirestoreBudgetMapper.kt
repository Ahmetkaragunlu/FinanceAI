package com.ahmetkaragunlu.financeai.feature.budget.data.remote

import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields

import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget

internal fun Budget.toFirebaseMap(): Map<String, Any?> = mapOf(
    "budgetType" to budgetType.name,
    FinancialFields.CATEGORY to category?.name,
    FinancialFields.AMOUNT_MINOR to MoneyAmounts.toMinor(amount, currencyCode),
    FinancialFields.CURRENCY_CODE to currencyCode,
    FinancialFields.LEGACY_AMOUNT to amount,
    "limitPercentage" to limitPercentage
)
