package com.ahmetkaragunlu.financeai.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.navigateSingleTopClear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTopBar(
    currentRoute: String,
    navController: NavHostController,

) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(topBarTitleForRoute(currentRoute)),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        },
        actions = {
            TopBarActionForRoute(currentRoute = currentRoute, navController = navController)
        },

        navigationIcon = {
            if (currentRoute != Screens.HomeScreen.route) {
                IconButton(
                    onClick = {
                        navController.popBackStack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.background))
    )
}


@Composable
private fun topBarTitleForRoute(currentRoute: String): Int {
    return when (currentRoute) {
        Screens.HomeScreen.route -> R.string.welcome
        Screens.TRANSACTION_HISTORY_SCREEN.route -> R.string.history_and_scheduled
        Screens.AiChatScreen.route -> R.string.ai_assistant
        Screens.AnalysisScreen.route -> R.string.analysis
        Screens.AddTransaction.route -> R.string.add
        Screens.DetailScreen.route -> R.string.detail_screen
        else -> R.string.welcome
    }
}

@Composable
private fun TopBarActionForRoute(
    currentRoute: String,
    navController: NavHostController,
) {
    when (currentRoute) {
        Screens.HomeScreen.route -> {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.account),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }
        }
    }
}