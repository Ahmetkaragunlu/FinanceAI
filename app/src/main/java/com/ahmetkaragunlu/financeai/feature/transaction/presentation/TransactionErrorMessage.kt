package com.ahmetkaragunlu.financeai.feature.transaction.presentation

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.error.dataErrorMessageRes

fun transactionFailure(
    error: Throwable,
    @StringRes operation: Int,
): TransactionActionResult.Failure =
    TransactionActionResult.Failure(
        operation,
        dataErrorMessageRes(error) ?: R.string.error_operation_retry,
    )
