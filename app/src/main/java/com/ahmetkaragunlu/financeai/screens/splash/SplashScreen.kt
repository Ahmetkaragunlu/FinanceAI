package com.ahmetkaragunlu.financeai.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ahmetkaragunlu.financeai.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.ahmetkaragunlu.financeai.navigation.Screens
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    LaunchedEffect(key1 = Unit) {
        delay(3000)
        navController.navigate(Screens.SignInScreen.route)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF304cd3),
                        Color(0xFFa63bee)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1080f, 1080f))),
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

