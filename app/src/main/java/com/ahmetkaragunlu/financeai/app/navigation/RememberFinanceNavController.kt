package com.ahmetkaragunlu.financeai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/** Older route ids cannot restore after feature package renames. Other saved state is kept. */
@Composable
internal fun rememberFinanceNavController(): NavHostController =
    key("typed-navigation-v3") { rememberNavController() }
