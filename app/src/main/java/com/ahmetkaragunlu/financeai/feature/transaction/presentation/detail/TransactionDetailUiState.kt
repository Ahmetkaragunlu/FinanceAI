package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

sealed interface TransactionDetailUiState {
    val transaction: Transaction?
        get() = null

    data object Loading : TransactionDetailUiState

    data object NotFound : TransactionDetailUiState

    data class Content(override val transaction: Transaction) : TransactionDetailUiState

    data class Error(@StringRes val messageRes: Int) : TransactionDetailUiState
}
