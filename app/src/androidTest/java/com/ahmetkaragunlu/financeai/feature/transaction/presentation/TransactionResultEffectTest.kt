package com.ahmetkaragunlu.financeai.feature.transaction.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.TransactionDetailScreen
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.TransactionDetailUiState
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.TransactionEditState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TransactionResultEffectTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun stoppedUiWaitsAndUsesTheLatestCallbackWhenStarted() {
        lateinit var owner: TestOwner
        var result by mutableStateOf<TransactionActionResult?>(TransactionActionResult.Saved)
        var useLatestCallback by mutableStateOf(false)
        var oldCalls = 0
        var currentCalls = 0
        compose.runOnIdle { owner = TestOwner() }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TransactionResultEffect(
                    result,
                    { result = null },
                    if (useLatestCallback) {
                        { currentCalls++ }
                    } else {
                        { oldCalls++ }
                    },
                )
            }
        }
        compose.runOnIdle {
            assertEquals(TransactionActionResult.Saved, result)
            useLatestCallback = true
        }
        compose.waitForIdle()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.runOnIdle {
            assertNull(result)
            assertEquals(0, oldCalls)
            assertEquals(1, currentCalls)
            owner.registry.currentState = Lifecycle.State.CREATED
        }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.runOnIdle { assertEquals(1, currentCalls) }
    }

    @Test
    fun pendingResultSurvivesCompositionReplacementButConsumedResultDoesNotRepeat() {
        lateinit var owner: TestOwner
        var result by mutableStateOf<TransactionActionResult?>(TransactionActionResult.Updated)
        var mounted by mutableStateOf(true)
        var calls = 0
        compose.runOnIdle { owner = TestOwner() }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                if (mounted) TransactionResultEffect(result, { result = null }) { calls++ }
            }
        }
        compose.runOnIdle { mounted = false }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(TransactionActionResult.Updated, result)
            assertEquals(0, calls)
            owner.registry.currentState = Lifecycle.State.STARTED
            mounted = true
        }
        compose.runOnIdle {
            assertNull(result)
            assertEquals(1, calls)
            mounted = false
        }
        compose.waitForIdle()
        compose.runOnIdle { mounted = true }
        compose.runOnIdle { assertEquals(1, calls) }
    }

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

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.CREATED }
        override val lifecycle: Lifecycle
            get() = registry
    }
}
