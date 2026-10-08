package com.ahmetkaragunlu.financeai.feature.auth.presentation.session

import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `sign out result waits for repository and remains until UI consumes it`() = runTest {
        val completed = CompletableDeferred<Unit>()
        var signOutCalls = 0
        val repository =
            FakeAuthRepository().apply {
                onSignOut = {
                    signOutCalls++
                    completed.await()
                }
            }
        val viewModel = SessionViewModel(repository)
        val store = ViewModelStore().apply { put("session", viewModel) }

        try {
            viewModel.performSignOut()
            viewModel.performSignOut()
            advanceUntilIdle()
            assertFalse(viewModel.signOutComplete)
            assertEquals(1, signOutCalls)
            completed.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.signOutComplete)
            viewModel.consumeSignOutResult()
            assertFalse(viewModel.signOutComplete)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `clearing the owner cancels sign out without publishing navigation success`() = runTest {
        val pending = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository().apply { onSignOut = { pending.await() } }
        val viewModel = SessionViewModel(repository)
        val store = ViewModelStore().apply { put("session", viewModel) }
        viewModel.performSignOut()
        advanceUntilIdle()
        store.clear()
        pending.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.signOutComplete)
    }
}
