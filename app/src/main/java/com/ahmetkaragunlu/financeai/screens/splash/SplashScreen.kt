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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    LaunchedEffect(key1 = Unit) {
        delay(3000)
        if (FirebaseAuth.getInstance().currentUser != null) {
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

