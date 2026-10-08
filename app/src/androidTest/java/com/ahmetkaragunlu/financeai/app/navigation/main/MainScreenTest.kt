package com.ahmetkaragunlu.financeai.app.navigation.main

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pendingUserNameHasNoPlaceholderAndBecomesTheExistingWelcomeTitle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = mutableStateOf("")
        compose.setContent { Text(MainScreen.HOME.localizedTitle(name.value)) }
        compose.onNodeWithText(context.getString(R.string.home)).assertIsDisplayed()
        compose.runOnIdle { name.value = "Ahmet" }
        compose.onNodeWithText(context.getString(R.string.welcome, "Ahmet")).assertIsDisplayed()
    }

    @Test fun otherScreenTitleIsIndependentOfUserName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.setContent { Text(MainScreen.HISTORY.localizedTitle("Ahmet")) }
        compose.onNodeWithText(context.getString(R.string.history_and_scheduled)).assertIsDisplayed()
    }
}
