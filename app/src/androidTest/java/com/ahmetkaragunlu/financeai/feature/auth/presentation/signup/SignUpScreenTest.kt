package com.ahmetkaragunlu.financeai.feature.auth.presentation.signup

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SignUpScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun verificationDialogSurvivesRestorationAfterItsResultHasBeenConsumed() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val title = context.getString(R.string.email_verification_sent)
        val viewModel = SignUpViewModel(RegistrationRepository())
        val viewModels = ViewModelStore().apply { put("signup", viewModel) }
        val restoration = StateRestorationTester(composeRule)

        try {
            restoration.setContent {
                MaterialTheme { SignUpRoute(viewModel = viewModel, onSignIn = {}) }
            }
            composeRule.runOnIdle { viewModel.registerUser() }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(title).assertIsDisplayed()
            composeRule.runOnIdle { assertEquals(AuthState.EMPTY, viewModel.authState.value) }

            restoration.emulateSavedInstanceStateRestore()

            composeRule.onNodeWithText(title).assertIsDisplayed()
            composeRule.runOnIdle { assertEquals(AuthState.EMPTY, viewModel.authState.value) }
        } finally {
            composeRule.runOnIdle { viewModels.clear() }
        }
    }

    private class RegistrationRepository : AuthRepository {
        override suspend fun registerUser(
            email: String,
            password: String,
            firstName: String,
            lastName: String,
        ) = Unit

        override suspend fun signIn(email: String, password: String): Unit = unexpected()

        override suspend fun refreshEmailVerification(): Boolean = unexpected()

        override suspend fun verifyUserAndSendResetEmail(
            email: String,
            firstName: String,
            lastName: String,
        ): Boolean = unexpected()

        override suspend fun confirmPasswordReset(oobCode: String, newPassword: String): Unit =
            unexpected()

        override suspend fun signInWithGoogle(idToken: String?): Unit = unexpected()

        override suspend fun isUserRegistered(email: String): Boolean = unexpected()

        override suspend fun signOut(): Unit = unexpected()

        override suspend fun getUserName(): String? = unexpected()

        private fun unexpected(): Nothing = error("Unexpected auth repository call")
    }
}
