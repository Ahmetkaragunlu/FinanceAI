package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.section

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import org.junit.Rule
import org.junit.Test

class BudgetWarningCardTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun suppliedWarningKeepsTheExistingHeadingAndSymbol() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val message = "Synthetic budget warning"
        compose.setContent { MaterialTheme { BudgetWarningCard(message = message) } }
        compose.onNodeWithText(context.getString(R.string.attention_budget_exceeded)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.warning_symbol)).assertIsDisplayed()
        compose.onNodeWithText(message).assertIsDisplayed()
    }
}
