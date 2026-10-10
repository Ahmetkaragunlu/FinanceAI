package com.ahmetkaragunlu.financeai.feature.home.presentation.suggestion

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType

sealed interface AiSuggestionState {
    data object Analyze : AiSuggestionState

    data object Planning : AiSuggestionState

    data object Healthy : AiSuggestionState

    data class NoBudget(val spent: Double) : AiSuggestionState

    data class GeneralExceeded(val limit: Double, val spent: Double, val percentage: Int) :
        AiSuggestionState

    data class GeneralNearLimit(val percentage: Int) : AiSuggestionState

    data class CategoryExceeded(val category: CategoryType, val limit: Double, val spent: Double) :
        AiSuggestionState

    data class CategoryNearLimit(val category: CategoryType, val percentage: Int) :
        AiSuggestionState
}
