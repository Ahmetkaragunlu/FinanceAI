package com.ahmetkaragunlu.financeai.app.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withStateAtLeast
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import kotlinx.coroutines.CancellationException

@Composable
fun SplashRoute(
    modifier: Modifier = Modifier,
    onReady: (Boolean) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val readyCallback by rememberUpdatedState(onReady)
    LaunchedEffect(viewModel, lifecycle) {
        val ready =
            try {
                viewModel.canOpenFinance()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        lifecycle.withStateAtLeast(Lifecycle.State.STARTED) { readyCallback(ready) }
    }
    SplashScreen(modifier)
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(color = colorResource(R.color.background)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ai),
            contentDescription = null,
            tint = FinanceColors.onAccent,
        )
    }
}
