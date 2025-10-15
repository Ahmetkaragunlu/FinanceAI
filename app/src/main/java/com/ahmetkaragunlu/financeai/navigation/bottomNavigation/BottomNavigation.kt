package com.ahmetkaragunlu.financeai.navigation.bottomNavigation

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                        FloatingActionButton(
                            onClick = {
                                navController.navigateSingleTopClear(Screens.AddTransaction.route)
                                      },
                            shape = CircleShape,
                            modifier = Modifier.offset(y = (-4).dp).size(70.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        }
                    },
                    label = null,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colorResource(R.color.background)
                    )
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
