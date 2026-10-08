package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import com.ahmetkaragunlu.financeai.core.time.DateFilter
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType

data class HistoryFilters(
    val date: DateFilter = DateFilter.ALL,
    val type: TransactionType? = null,
    val category: CategoryType? = null,
)
