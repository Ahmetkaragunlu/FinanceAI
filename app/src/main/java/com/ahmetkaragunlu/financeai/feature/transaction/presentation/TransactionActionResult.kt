package com.ahmetkaragunlu.financeai.feature.transaction.presentation

import androidx.annotation.StringRes

sealed interface TransactionActionResult {
    data object Saved : TransactionActionResult

    data object Updated : TransactionActionResult

    data object Deleted : TransactionActionResult

    data object PhotoDeleted : TransactionActionResult

    data class Failure(@StringRes val messageRes: Int, @StringRes val detailRes: Int? = null) :
        TransactionActionResult
}
