package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType

data class HistoryFilters(
    @StringRes val dateResId: Int = R.string.date,
    val type: TransactionType? = null,
    val category: CategoryType? = null,
)
