package com.ahmetkaragunlu.financeai.core.ui.effect

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withStateAtLeast

/** Wait for a visible UI before displaying and acknowledging an existing Toast message. */
@Composable
fun ToastMessageEffect(@StringRes messageRes: Int?, onConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val consume by rememberUpdatedState(onConsumed)
    LaunchedEffect(messageRes, lifecycle) {
        if (messageRes != null)
            lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
                consume()
            }
    }
}
