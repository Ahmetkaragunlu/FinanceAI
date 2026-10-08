package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType

data class TransactionEditState(
    val amount: String,
    val note: String,
    val category: CategoryType?,
    val categories: List<CategoryType>,
)
