package com.ahmetkaragunlu.financeai.navigation.bottomnavigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.navigation.Screens

sealed class BottomIcon {
    data class Vector(val icon: ImageVector) : BottomIcon()
    data class Drawable(val resId: Int) : BottomIcon()
}

data class BottomNavItem(
    @StringRes val label: Int,
    val icon: BottomIcon,
    val screen: String
)

val bottomNavItem = listOf(
    BottomNavItem(
        label = R.string.home,
        icon = BottomIcon.Vector(Icons.Default.Home),
        screen = Screens.HomeScreen.route
    ),
    BottomNavItem(
        label = R.string.history,
        icon = BottomIcon.Vector(Icons.Default.History),
        screen = Screens.HISTORY_SCREEN.route
    ),
    BottomNavItem(
        label = R.string.add,
        icon = BottomIcon.Vector(Icons.Default.Add),
        screen = Screens.AddTransaction.route
    ),
    BottomNavItem(
        label = R.string.ai_assistant,
        icon = BottomIcon.Drawable(R.drawable.ai_icon),
        screen = Screens.AiChatScreen.route ),
    BottomNavItem(
        label = R.string.analysis,
        icon = BottomIcon.Vector(Icons.Default.Analytics),
        screen = Screens.AnalysisScreen.route
    )
)
