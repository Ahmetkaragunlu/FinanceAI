package com.ahmetkaragunlu.financeai.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ahmetkaragunlu.financeai.feature.home.navigation.HomeDestination
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.TransactionDetailDestination
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RememberFinanceNavControllerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun newTypedControllerRestoresDetailArgumentsAfterRecreation() {
        lateinit var controller: NavHostController
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            controller = rememberFinanceNavController()
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

    @Test
    fun legacyControllerStateIsDroppedWithoutClearingUnrelatedSavedState() {
        lateinit var controller: NavHostController
        var legacy = true
        var retainedValue = 0
        lateinit var updateRetainedValue: () -> Unit
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            var value by rememberSaveable { mutableIntStateOf(42) }
            retainedValue = value
            updateRetainedValue = { value = 77 }
            controller = if (legacy) rememberNavController() else rememberFinanceNavController()
            if (legacy) {
                NavHost(controller, startDestination = LegacyHome) {
                    composable<LegacyHome> {}
                    composable<LegacyDetail> {}
                }
            } else {
                NavHost(controller, startDestination = HomeDestination) {
                    composable<HomeDestination> {}
                    composable<TransactionDetailDestination> {}
                }
            }
        }
        compose.runOnIdle {
            controller.navigate(LegacyDetail(42))
            updateRetainedValue()
        }
        // The flag is intentionally not Compose state: only the recreated composition sees it.
        legacy = false
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertTrue(controller.currentDestination!!.hasRoute<HomeDestination>())
            assertNull(controller.previousBackStackEntry)
            assertEquals(77, retainedValue)
        }
    }

    @Serializable @SerialName("HomeScreen") private data object LegacyHome

    @Serializable
    @SerialName("Detail_Screen")
    private data class LegacyDetail(val transactionId: Int)
}
