package com.ahmetkaragunlu.financeai.app.navigation

import androidx.navigation.NavController

/** Root changes discard the previous auth/account stack without retaining tab state. */
inline fun <reified T : Any> NavController.switchRoot(route: T) {
    navigateSingleTopClear(route)
}

inline fun <reified T : Any> NavController.navigateSingleTopClear(route: T) {
    this.navigate(route) {
        // Keep the graph itself; preserve the existing reset-on-navigation behaviour.
        popUpTo(graph.id) { inclusive = false }
        launchSingleTop = true
    }
}
