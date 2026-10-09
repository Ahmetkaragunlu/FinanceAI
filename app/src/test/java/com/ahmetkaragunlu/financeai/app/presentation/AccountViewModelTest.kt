package com.ahmetkaragunlu.financeai.app.presentation

import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test fun storedTurkishSpellingAndBlankNamesAreNotReformatted() = runTest {
        val session = AccountSession()
        var name: String? = null
        val repository = FakeAuthRepository().apply { onUserName = { name } }
        val model = AccountViewModel(session, repository)
        val store = ViewModelStore().apply { put("account", model) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.userName.collect() }
            listOf("İpek", "İPEK", "ahmet", "", null).forEachIndexed { i, value ->
                name = value
                session.activate("owner-$i", "USD")
                runCurrent()
                assertEquals(value.orEmpty(), model.userName.value)
            }
        } finally { store.clear() }
    }

    @Test
    fun `header name loads once for shared collectors and clears on sign out`() = runTest {
        val session = AccountSession()
        var requests = 0
        val repository = FakeAuthRepository().apply {
            onUserName = { requests++; "aHMET" }
        }
        val viewModel = AccountViewModel(session, repository)
        val store = ViewModelStore().apply { put("account", viewModel) }
        try {
            repeat(2) {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.userName.collect()
                }
            }
            runCurrent()
            assertEquals(0, requests)
            assertEquals("", viewModel.userName.value)

            session.activate("A", "USD")
            runCurrent()
            assertEquals("aHMET", viewModel.userName.value)
            assertEquals(1, requests)

            session.deactivate()
            runCurrent()
            assertEquals("", viewModel.userName.value)
            assertEquals(1, requests)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `account switch clears the previous name while the new lookup is pending`() = runTest {
        val session = AccountSession().apply { activate("A", "USD") }
        val secondName = CompletableDeferred<String?>()
        val repository = FakeAuthRepository().apply {
            onUserName = {
                if (session.requireAccount().ownerId == "A") "alice" else secondName.await()
            }
        }
        val viewModel = AccountViewModel(session, repository)
        val store = ViewModelStore().apply { put("account", viewModel) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.userName.collect()
            }
            runCurrent()
            assertEquals("alice", viewModel.userName.value)

            session.activate("B", "EUR")
            runCurrent()
            assertEquals("", viewModel.userName.value)

            secondName.complete("bob")
            runCurrent()
            assertEquals("bob", viewModel.userName.value)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `late lookup from the previous account cannot replace the current header`() = runTest {
        val session = AccountSession().apply { activate("A", "USD") }
        val firstName = CompletableDeferred<String?>()
        val repository = FakeAuthRepository().apply {
            onUserName = {
                if (session.requireAccount().ownerId == "A") firstName.await() else "bob"
            }
        }
        val viewModel = AccountViewModel(session, repository)
        val store = ViewModelStore().apply { put("account", viewModel) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.userName.collect()
            }
            runCurrent()

            session.activate("B", "EUR")
            runCurrent()
            assertEquals("bob", viewModel.userName.value)

            firstName.complete("alice")
            runCurrent()
            assertEquals("bob", viewModel.userName.value)
        } finally {
            store.clear()
        }
    }
}
