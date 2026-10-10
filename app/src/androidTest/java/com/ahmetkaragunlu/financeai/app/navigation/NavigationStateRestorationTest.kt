package com.ahmetkaragunlu.financeai.app.navigation

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ahmetkaragunlu.financeai.feature.home.destination.HomeDestination
import com.ahmetkaragunlu.financeai.feature.transaction.destination.TransactionDetailDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationStateRestorationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun typedControllerRestoresDetailArgumentsAfterRecreation() {
        lateinit var controller: NavHostController
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = HomeDestination) {
                composable<HomeDestination> {}
                composable<TransactionDetailDestination> {}
            }
        }
        compose.runOnIdle { controller.navigate(TransactionDetailDestination(42)) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(
                42,
                controller.currentBackStackEntry!!
                    .toRoute<TransactionDetailDestination>()
                    .transactionId,
            )
            assertTrue(controller.previousBackStackEntry!!.destination.hasRoute<HomeDestination>())
        }
    }
}
