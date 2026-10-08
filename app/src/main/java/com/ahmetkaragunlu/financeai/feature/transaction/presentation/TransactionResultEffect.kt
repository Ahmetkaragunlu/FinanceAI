package com.ahmetkaragunlu.financeai.feature.transaction.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withStateAtLeast

/** Pending results belong to the ViewModel; only the current visible UI performs UI effects. */
@Composable
fun TransactionResultEffect(
    result: TransactionActionResult?,
    onConsumed: () -> Unit,
    onSuccess: (TransactionActionResult) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val consume by rememberUpdatedState(onConsumed)
    val success by rememberUpdatedState(onSuccess)
    LaunchedEffect(result, lifecycle) {
        if (result != null) {
            lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                consume()
                when (result) {
                    is TransactionActionResult.Failure -> {
                        val message =
                            result.detailRes?.let {
                                context.getString(result.messageRes, context.getString(it))
                            } ?: context.getString(result.messageRes)
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    else -> success(result)
                }
            }
        }
    }
}
