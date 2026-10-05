package com.ahmetkaragunlu.financeai.feature.budget.data.mapper

import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.budget.data.local.BudgetEntity
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    firestoreId = firestoreId,
    budgetType = budgetType,
    category = category,
    amount = MoneyAmounts.toMajor(amountMinor, currencyCode),
    limitPercentage = limitPercentage,
    ownerId = ownerId,
    currencyCode = currencyCode,
    syncedToFirebase = syncedToFirebase
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    firestoreId = firestoreId,
    budgetType = budgetType,
    category = category,
    amountMinor = MoneyAmounts.toMinor(amount, currencyCode),
    limitPercentage = limitPercentage,
    ownerId = ownerId,
    currencyCode = currencyCode,
    syncedToFirebase = syncedToFirebase
)
