package com.ahmetkaragunlu.financeai.app.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.app.navigation.Screens
import kotlinx.coroutines.CancellationException

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = Unit) {
        val ready = try { viewModel.canOpenFinance() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { false }
        if (ready) {
            navController.navigate(Screens.MAIN_GRAPH.route) {
                popUpTo(Screens.SplashScreen.route) { inclusive = true }
            }
        } else {
           navController.navigate(Screens.SignInScreen.route)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.background)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ai),
            contentDescription = null,
            tint = Color.White
        )
    }
}
