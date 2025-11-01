package com.ahmetkaragunlu.financeai.navigation.bottomnavigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.navigateSingleTopClear

@Composable
fun BottomBar(
    navController: NavHostController,
    currentRoute: String,
    modifier: Modifier = Modifier,
) {
    val isOnAddTransactionScreen = currentRoute == Screens.AddTransaction.route
    NavigationBar(
        containerColor = colorResource(R.color.background),
        tonalElevation = 8.dp,
        modifier = modifier
    ) {
        bottomNavItem.forEachIndexed { index, item ->
            if (index == 2) {
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = {
                        Box(
                            modifier = Modifier
                                .offset(y = (-4).dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFb55ebf),
                                            Color(0xFF36a2cc),
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(250f, 0f)
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    if (!isOnAddTransactionScreen) {
                                        navController.navigateSingleTopClear(Screens.AddTransaction.route)
                                    }
                                },
                                modifier = Modifier.size(70.dp),
                                containerColor = Color.Transparent,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp,
                                    focusedElevation = 0.dp,
                                    hoveredElevation = 0.dp
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    label = null,
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)

                )
            } else {
                NavigationBarItem(
                    icon = {
                        when (val ic = item.icon) {
                            is BottomIcon.Vector ->
                                Icon(
                                    imageVector = ic.icon,
                                    contentDescription = null
                                )
                            is BottomIcon.Drawable ->
                                Icon(
                                    painter = painterResource(ic.resId),
                                    contentDescription = null
                                )
                        }
                    },
                    label = { Text(text = stringResource(item.label)) },
                    selected = currentRoute == item.screen,
                    onClick = {
                        if (currentRoute != item.screen && item.screen.isNotEmpty()) {
                            navController.navigateSingleTopClear(item.screen)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = colorResource(R.color.background)
                    )
                )
            }
        }
    }
}