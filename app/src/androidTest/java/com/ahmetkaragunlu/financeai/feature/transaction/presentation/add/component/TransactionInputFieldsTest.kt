package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransactionInputFieldsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun controlledInputsAndCategorySelectionKeepTheExpenseAndIncomeOptionsSeparate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val amount = mutableStateOf("")
        val note = mutableStateOf("")
        val category = mutableStateOf<CategoryType?>(null)
        val type = mutableStateOf(TransactionType.EXPENSE)
        compose.setContent {
            MaterialTheme {
                Column {
                    TransactionInputFields(
                        amount.value, note.value, category.value, type.value,
                        { amount.value = it }, { note.value = it }, { category.value = it },
                    )
                }
            }
        }

        compose.onAllNodes(hasSetTextAction())[0].performTextInput("25.50")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("receipt")
        compose.onNodeWithText(context.getString(R.string.select_category)).performClick()
        compose.onNodeWithText(context.getString(R.string.category_salary)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.category_food)).performClick()
        compose.runOnIdle {
            assertEquals("25.50", amount.value)
            assertEquals("receipt", note.value)
            assertEquals(CategoryType.FOOD, category.value)
            type.value = TransactionType.INCOME
            category.value = null
        }

        compose.onNodeWithText(context.getString(R.string.select_category)).performClick()
        compose.onNodeWithText(context.getString(R.string.category_food)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.category_salary)).performClick()
        compose.runOnIdle { assertEquals(CategoryType.SALARY, category.value) }
    }
}
