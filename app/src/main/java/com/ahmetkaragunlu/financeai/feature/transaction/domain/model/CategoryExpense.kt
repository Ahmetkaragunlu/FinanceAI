package com.ahmetkaragunlu.financeai.feature.transaction.domain.model

data class CategoryExpense(
    // Unknown legacy projections stay distinct from an explicit OTHER category.
    val category: CategoryType?,
    val totalAmount: Double
)
