package com.ahmetkaragunlu.financeai.app.navigation

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.app.navigation.main.MainScreen
import com.ahmetkaragunlu.financeai.app.navigation.main.mainScreen
import com.ahmetkaragunlu.financeai.feature.auth.navigation.PasswordResetDestination
import com.ahmetkaragunlu.financeai.feature.auth.navigation.SignInDestination
import com.ahmetkaragunlu.financeai.feature.home.navigation.HomeDestination
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.TransactionDetailDestination
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.TransactionHistoryDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypedNavigationTest {
    @Test
    fun savedTypedStackRestoresTheSameDetailAndPendingPrompt() =
        runBlocking(Dispatchers.Main) {
            withController { original ->
                original.graph =
                    original.createGraph(startDestination = HomeDestination) {
                        composable<HomeDestination> {}
                        composable<TransactionDetailDestination> {}
                    }
                original.navigate(TransactionDetailDestination(42))
                original.currentBackStackEntry!!.savedStateHandle["pending_prompt"] =
                    "synthetic prompt"
                val saved = checkNotNull(original.saveState())
                withController { restored ->
                    restored.restoreState(saved)
                    restored.graph =
                        restored.createGraph(startDestination = HomeDestination) {
                            composable<HomeDestination> {}
                            composable<TransactionDetailDestination> {}
                        }
                    assertEquals(
                        42,
                        restored.currentBackStackEntry!!
                            .toRoute<TransactionDetailDestination>()
                            .transactionId,
                    )
                    assertEquals(
                        "synthetic prompt",
                        restored.currentBackStackEntry!!
                            .savedStateHandle
                            .get<String>("pending_prompt"),
                    )
                    assertTrue(
                        restored.previousBackStackEntry!!.destination.hasRoute<HomeDestination>()
                    )
                    assertTrue(restored.popBackStack())
                    assertTrue(restored.currentDestination!!.hasRoute<HomeDestination>())
                }
            }
        }

    @Test
    fun repeatedRootSwitchDoesNotDuplicateOrSaveThePreviousStack() =
        runBlocking(Dispatchers.Main) {
            withController { controller ->
                controller.graph =
                    controller.createGraph(startDestination = SignInDestination) {
                        composable<SignInDestination> {}
                        composable<MainDestination> {}
                    }
                controller.switchRoot(MainDestination)
                controller.switchRoot(MainDestination)
                assertTrue(controller.currentDestination!!.hasRoute<MainDestination>())
                assertNull(controller.previousBackStackEntry)
                controller.switchRoot(SignInDestination)
                assertTrue(controller.currentDestination!!.hasRoute<SignInDestination>())
                assertNull(controller.previousBackStackEntry)
                assertFalse(controller.popBackStack())
            }
        }

    @Test
    fun detailIdsRoundTripThroughBothEntryAndSavedStateWithoutAFallbackId() =
        runBlocking(Dispatchers.Main) {
            withController { controller ->
                controller.graph =
                    controller.createGraph(startDestination = HomeDestination) {
                        composable<HomeDestination> {}
                        composable<TransactionDetailDestination> {}
                        composable<TransactionHistoryDestination> {}
                    }
                controller.navigate(TransactionDetailDestination(42))
                val entry = controller.currentBackStackEntry!!
                assertEquals(42, entry.toRoute<TransactionDetailDestination>().transactionId)
                assertEquals(
                    42,
                    entry.savedStateHandle.toRoute<TransactionDetailDestination>().transactionId,
                )
                assertEquals(MainScreen.DETAIL, entry.destination.mainScreen())
                controller.navigateSingleTopClear(TransactionHistoryDestination)
                assertTrue(
                    controller.currentDestination!!.hasRoute<TransactionHistoryDestination>()
                )
                assertNull(controller.previousBackStackEntry)
            }
        }

    @Test
    fun resetCodeEncodingPreservesReservedCharacters() =
        runBlocking(Dispatchers.Main) {
            withController { controller ->
                controller.graph =
                    controller.createGraph(startDestination = SignInDestination) {
                        composable<SignInDestination> {}
                        composable<PasswordResetDestination> {}
                    }
                controller.navigate(PasswordResetDestination("code+with/slash?and=value"))
                assertEquals(
                    "code+with/slash?and=value",
                    controller.currentBackStackEntry!!.toRoute<PasswordResetDestination>().oobCode,
                )
            }
        }

    private inline fun withController(test: (TestNavHostController) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ViewModelStore()
        val owner =
            object : LifecycleOwner {
                override val lifecycle =
                    LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
            }
        val controller =
            TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
                setViewModelStore(store)
                setLifecycleOwner(owner)
            }
        try {
            test(controller)
        } finally {
            store.clear()
        }
    }
}
