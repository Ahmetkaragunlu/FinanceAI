package com.ahmetkaragunlu.financeai.feature.auth.presentation.session

import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `navigation callback waits for repository sign out to return`() = runTest {
        val completed = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository().apply { onSignOut = { completed.await() } }
        val viewModel = SessionViewModel(repository)
        var navigationCalls = 0

        viewModel.performSignOut { navigationCalls++ }
        advanceUntilIdle()
        assertEquals(0, navigationCalls)
        completed.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, navigationCalls)
    }
}
