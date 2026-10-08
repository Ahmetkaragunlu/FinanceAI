package com.ahmetkaragunlu.financeai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/** Legacy SerialName ids cannot be restored into the new typed graph. Other saved state is kept. */
@Composable
internal fun rememberFinanceNavController(): NavHostController =
    key("typed-navigation-v2") { rememberNavController() }
