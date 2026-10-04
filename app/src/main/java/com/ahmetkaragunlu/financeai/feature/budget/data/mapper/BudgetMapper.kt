package com.ahmetkaragunlu.financeai.feature.budget.data.mapper

import com.ahmetkaragunlu.financeai.feature.budget.data.local.BudgetEntity
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    firestoreId = firestoreId,
    budgetType = budgetType,
    category = category,
    amount = amount,
    limitPercentage = limitPercentage,
    syncedToFirebase = syncedToFirebase
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    firestoreId = firestoreId,
    budgetType = budgetType,
    category = category,
    amount = amount,
    limitPercentage = limitPercentage,
    syncedToFirebase = syncedToFirebase
)
