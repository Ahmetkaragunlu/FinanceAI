package com.ahmetkaragunlu.financeai.feature.auth.presentation.signin

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.test.espresso.Espresso.pressBackUnconditionally
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceAITheme
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.auth.domain.usecase.SignInWithGoogle
import com.ahmetkaragunlu.financeai.feature.auth.domain.usecase.SignInWithPassword
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class SignInRouteTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun systemBackKeepsTheSignInRouteAndItsDraftWithoutStartingAuthentication() {
        val repository = Mockito.mock(AuthRepository::class.java)
        val viewModel = SignInViewModel(SignInWithGoogle(repository), SignInWithPassword(repository))
        val store = ViewModelStore().apply { put("sign-in", viewModel) }
        val email = "draft@example.test"
        try {
            composeRule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
            composeRule.setContent {
                FinanceAITheme {
                    SignInRoute(
                        viewModel = viewModel,
                        onSignedIn = { error("Unexpected authentication") },
                        onForgotPassword = { error("Unexpected navigation") },
                        onSignUp = { error("Unexpected navigation") },
                    )
                }
            }
            composeRule.runOnIdle { viewModel.updateEmail(email) }
            composeRule.onNodeWithText(email).assertIsDisplayed()

            pressBackUnconditionally()

            composeRule.onNodeWithText(email).assertIsDisplayed()
            composeRule.runOnIdle {
                assertFalse(composeRule.activity.isFinishing)
                assertEquals(email, viewModel.inputEmail)
                Mockito.verifyNoInteractions(repository)
            }
        } finally {
            composeRule.runOnIdle { store.clear() }
        }
    }
}
