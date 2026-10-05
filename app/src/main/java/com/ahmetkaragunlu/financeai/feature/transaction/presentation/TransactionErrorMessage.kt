package com.ahmetkaragunlu.financeai.feature.transaction.presentation

import android.content.Context
import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.error.dataErrorMessageRes

fun transactionFailureMessage(context: Context, error: Throwable, @StringRes operation: Int): String =
    context.getString(operation, context.getString(dataErrorMessageRes(error) ?: R.string.error_operation_retry))
