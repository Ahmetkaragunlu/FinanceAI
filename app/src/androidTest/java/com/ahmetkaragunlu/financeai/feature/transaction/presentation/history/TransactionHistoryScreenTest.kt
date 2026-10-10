package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.format.formatAsCurrency
import com.ahmetkaragunlu.financeai.core.ui.component.LocalAccountCurrency
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransactionHistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun switchingTabsAndRecreationKeepTheHistoryPositionAndClickedRecordIdentity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val transactions = (1..40).map { id ->
            Transaction(id = id, amount = id.toDouble(), transaction = TransactionType.EXPENSE,
                category = CategoryType.FOOD, note = "", date = 100L)
        }
        var selected: Int? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Screen(transactions) { selected = it } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
        val visibleAmount = transactions[20].amount.formatAsCurrency("USD")
        compose.onNodeWithText(visibleAmount).performClick()
        compose.runOnIdle { assertEquals(21, selected) }
        compose.onNodeWithText(context.getString(R.string.scheduled)).performClick()
        compose.onNodeWithText("Scheduled content").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Scheduled content").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.history)).performClick()
        compose.onNodeWithText(visibleAmount).assertIsDisplayed()
        compose.onNodeWithText(transactions.first().amount.formatAsCurrency("USD")).assertDoesNotExist()
    }

    @Test
    fun emptyHistoryAndProvidedScheduledContentRemainSeparate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.setContent { Screen(emptyList()) {} }
        compose.onNodeWithText(context.getString(R.string.no_record_found)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.scheduled)).performClick()
        compose.onNodeWithText("Scheduled content").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.no_record_found)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.history)).performClick()
        compose.onNodeWithText(context.getString(R.string.no_record_found)).assertIsDisplayed()
    }

    @Composable
    private fun Screen(transactions: List<Transaction>, onClick: (Int) -> Unit) {
        CompositionLocalProvider(LocalAccountCurrency provides "USD") {
            MaterialTheme {
                TransactionHistoryScreen(
                    transactions = transactions,
                    filters = HistoryFilters(),
                    showCategoryError = false,
                    onDateSelected = {},
                    onTypeSelected = {},
                    onCategorySelected = {},
                    onCategoryMenuRequested = { false },
                    onTransactionClick = onClick,
                    scheduledContent = { Text("Scheduled content") },
                )
            }
        }
    }
}
