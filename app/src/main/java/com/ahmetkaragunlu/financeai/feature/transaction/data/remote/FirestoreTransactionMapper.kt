package com.ahmetkaragunlu.financeai.feature.transaction.data.remote

import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields

import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

internal fun Transaction.toFirebaseMap(): Map<String, Any?> = mapOf(
    FinancialFields.AMOUNT_MINOR to MoneyAmounts.toMinor(amount, currencyCode),
    FinancialFields.CURRENCY_CODE to currencyCode,
    FinancialFields.LEGACY_AMOUNT to amount,
    TransactionFields.TYPE to transaction.name,
    FinancialFields.NOTE to note,
    TransactionFields.DATE to date,
    FinancialFields.CATEGORY to category.name,
    FinancialFields.LOCATION_FULL to locationFull,
    FinancialFields.LOCATION_SHORT to locationShort,
    FinancialFields.LATITUDE to latitude,
    FinancialFields.LONGITUDE to longitude
)
