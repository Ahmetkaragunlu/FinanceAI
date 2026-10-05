package com.ahmetkaragunlu.financeai.feature.budget.domain.model

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType

data class Budget(
    val id: Int = 0,
    val firestoreId: String = "",
    val budgetType: BudgetType,
    val category: CategoryType? = null,
    val amount: Double = 0.0,
    val limitPercentage: Double? = null,
    val ownerId: String = "",
    val currencyCode: String = "XXX",
    val syncedToFirebase: Boolean = false
)
