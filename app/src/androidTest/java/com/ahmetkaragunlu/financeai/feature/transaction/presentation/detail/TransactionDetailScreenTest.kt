package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.LocalAccountCurrency
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TransactionDetailScreenTest {
    @get:Rule val compose = createComposeRule()
    private val transaction = Transaction(
        firestoreId = "receipt", amount = 10.0, transaction = TransactionType.EXPENSE,
        category = CategoryType.FOOD, note = "receipt", date = 100L,
    )

    @Test
    fun successfulDeletionStillNavigatesWhenRepositoryAlreadyEmittedNotFound() {
        var result by mutableStateOf<TransactionActionResult?>(TransactionActionResult.Deleted)
        var navigations = 0
        compose.setContent {
            MaterialTheme {
                TransactionDetailScreen(
                    state = TransactionDetailUiState.NotFound,
                    editState = TransactionEditState("", "", null, emptyList()),
                    onEditRequested = { false },
                    onAmountChanged = {},
                    onNoteChanged = {},
                    onCategoryChanged = {},
                    onUpdateRequested = {},
                    onDeleteRequested = {},
                    actionResult = result,
                    onResultConsumed = { result = null },
                    onDeleted = { navigations++ },
                    onDeletePhoto = {},
                    onCameraClick = {},
                    onGalleryClick = {},
                )
            }
        }
        compose.runOnIdle {
            assertNull(result)
            assertEquals(1, navigations)
        }
    }

    @Test
    fun editRequiresAnAvailableDraftAndClosesOnlyAfterSuccessfulUpdate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val allowed = mutableStateOf(false)
        val result = mutableStateOf<TransactionActionResult?>(null)
        compose.setContent {
            Screen(
                onEditRequested = { allowed.value },
                onUpdateRequested = {
                    result.value = TransactionActionResult.Failure(R.string.error_transaction_save_failed)
                },
                result = result.value,
                onResultConsumed = { result.value = null },
            )
        }
        compose.onNodeWithText(context.getString(R.string.edit)).performClick()
        compose.onNodeWithText(context.getString(R.string.edit_transaction_title)).assertDoesNotExist()
        compose.runOnIdle { allowed.value = true }
        compose.onNodeWithText(context.getString(R.string.edit)).performClick()
        compose.onNodeWithText(context.getString(R.string.edit_transaction_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.save)).performClick()
        compose.onNodeWithText(context.getString(R.string.edit_transaction_title)).assertIsDisplayed()
        compose.runOnIdle { result.value = TransactionActionResult.Updated }
        compose.onNodeWithText(context.getString(R.string.edit_transaction_title)).assertDoesNotExist()
    }

    @Test
    fun deleteConfirmationWaitsForSuccessAndStillNavigatesAfterTheRecordDisappears() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = mutableStateOf<TransactionDetailUiState>(TransactionDetailUiState.Content(transaction))
        val result = mutableStateOf<TransactionActionResult?>(null)
        var requests = 0
        var navigations = 0
        compose.setContent {
            Screen(
                state = state.value,
                onDeleteRequested = { requests++ },
                result = result.value,
                onResultConsumed = { result.value = null },
                onDeleted = { navigations++ },
            )
        }
        compose.onNodeWithText(context.getString(R.string.delete)).performClick()
        compose.runOnIdle { assertEquals(0, requests) }
        compose.onNode(hasText(context.getString(R.string.delete)) and hasAnyAncestor(isDialog()))
            .performClick()
        compose.onNodeWithText(context.getString(R.string.delete_transaction_title)).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, requests)
            assertEquals(0, navigations)
            state.value = TransactionDetailUiState.NotFound
            result.value = TransactionActionResult.Deleted
        }
        compose.runOnIdle {
            assertEquals(1, navigations)
            assertNull(result.value)
        }
    }

    @Composable
    private fun Screen(
        state: TransactionDetailUiState = TransactionDetailUiState.Content(transaction),
        onEditRequested: () -> Boolean = { true },
        onUpdateRequested: () -> Unit = {},
        onDeleteRequested: () -> Unit = {},
        result: TransactionActionResult? = null,
        onResultConsumed: () -> Unit = {},
        onDeleted: () -> Unit = {},
    ) {
        CompositionLocalProvider(LocalAccountCurrency provides "USD") {
            MaterialTheme {
                TransactionDetailScreen(
                    state = state,
                    editState = TransactionEditState("10", "receipt", CategoryType.FOOD, listOf(CategoryType.FOOD)),
                    onEditRequested = onEditRequested,
                    onAmountChanged = {},
                    onNoteChanged = {},
                    onCategoryChanged = {},
                    onUpdateRequested = onUpdateRequested,
                    onDeleteRequested = onDeleteRequested,
                    actionResult = result,
                    onResultConsumed = onResultConsumed,
                    onDeleted = onDeleted,
                    onDeletePhoto = {},
                    onCameraClick = {},
                    onGalleryClick = {},
                )
            }
        }
    }
}
