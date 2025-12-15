package com.ahmetkaragunlu.financeai.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.ahmetkaragunlu.financeai.MainActivity
import com.ahmetkaragunlu.financeai.navigation.Screens

@Composable
 fun HandleDeepLinks(mainNavController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val activity = context as? MainActivity
                val data = activity?.intent?.data
                if (data?.scheme == MainNavConstants.DEEP_LINK_SCHEME &&
                    data.host == MainNavConstants.DEEP_LINK_HOST) {
                    when (data.path) {
                        MainNavConstants.DEEP_LINK_SCHEDULE_PATH -> {
                            mainNavController.navigate(Screens.ScheduledTransactionScreen.route) {
                                popUpTo(Screens.HomeScreen.route) { inclusive = false }
                                launchSingleTop = true
                            }
                            activity.intent.data = null
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}