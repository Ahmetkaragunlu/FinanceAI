package com.ahmetkaragunlu.financeai.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.ahmetkaragunlu.financeai.R

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    BackHandler{}
    Column(
        modifier = modifier.fillMaxSize().background(color = colorResource(R.color.background))
    ) {

    }
}