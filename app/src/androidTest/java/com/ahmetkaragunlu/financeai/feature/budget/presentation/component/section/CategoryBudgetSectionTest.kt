package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.section

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.LocalAccountCurrency
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetEvent
import com.ahmetkaragunlu.financeai.feature.budget.presentation.CategoryBudgetState
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryBudgetSectionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun orderedCardsDispatchEditAndDeleteForTheirOwnStateAndId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val categories = listOf(
            CategoryBudgetState(11, CategoryType.FOOD, BudgetType.CATEGORY_AMOUNT,
                50.0, 10.0, progress = 0.2f, isOverBudget = false, percentageUsed = 20),
            CategoryBudgetState(22, CategoryType.TRANSPORT, BudgetType.CATEGORY_PERCENTAGE,
                100.0, 120.0, limitPercentage = 25.0, progress = 1.2f,
                isOverBudget = true, percentageUsed = 120),
        )
        val events = mutableListOf<BudgetEvent>()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAccountCurrency provides "USD") {
                    CategoryBudgetSection(categories = categories, onEvent = { events += it })
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.category_food)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.category_transport)).assertIsDisplayed()
        // Each card retains edit then delete; no test-only tags or visible UI changes are needed.
        val actions = compose.onAllNodes(hasClickAction())
        actions.assertCountEquals(4)
        for (index in 0..3) actions[index].performClick()
        compose.runOnIdle {
            assertEquals(listOf(
                BudgetEvent.OnEditCategoryClick(categories[0]),
                BudgetEvent.OnDeleteClick(11),
                BudgetEvent.OnEditCategoryClick(categories[1]),
                BudgetEvent.OnDeleteClick(22),
            ), events)
        }
    }
}
