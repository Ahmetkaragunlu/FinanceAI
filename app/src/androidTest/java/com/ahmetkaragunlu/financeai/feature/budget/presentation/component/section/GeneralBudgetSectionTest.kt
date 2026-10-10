package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.section

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.LocalAccountCurrency
import com.ahmetkaragunlu.financeai.feature.budget.presentation.GeneralBudgetState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GeneralBudgetSectionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyAndFilledStatesKeepTheirExistingCreateAndEditActions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = mutableStateOf<GeneralBudgetState?>(null)
        val edits = mutableListOf<GeneralBudgetState>()
        var creations = 0
        val budget = GeneralBudgetState(7, 100.0, 25.0, 75.0, 0.25f, 200.0, 25.0)
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAccountCurrency provides "USD") {
                    GeneralBudgetSection(
                        state = state.value,
                        onEditClick = { edits += it },
                        onCreateClick = { creations++ },
                    )
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.not_set)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.create)).performClick()
        compose.runOnIdle {
            assertEquals(1, creations)
            assertEquals(emptyList<GeneralBudgetState>(), edits)
            state.value = budget
        }
        compose.onNodeWithText(context.getString(R.string.this_month_budget)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.edit)).performClick()
        compose.runOnIdle {
            assertEquals(1, creations)
            assertEquals(listOf(budget), edits)
        }
    }
}
