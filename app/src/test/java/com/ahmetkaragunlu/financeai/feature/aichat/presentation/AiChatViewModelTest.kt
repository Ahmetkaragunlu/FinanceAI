package com.ahmetkaragunlu.financeai.feature.aichat.presentation

import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiRequest
import com.ahmetkaragunlu.financeai.feature.aichat.domain.repository.AiRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private class Repository : AiRepository {
        val requests = mutableListOf<AiRequest>()
        var action: suspend () -> Unit = {}
        override fun observeChatHistory() = flowOf(emptyList<AiMessage>())
        override suspend fun sendMessage(request: AiRequest) { requests += request; action() }
    }
    @Test fun failedDraftRetriesTheSameRequestAndLoadingPreventsDuplicateSends() = runTest(main.dispatcher) {
        val repository = Repository().apply { action = { throw AiException.Unavailable() } }
        val session = AccountSession().apply { activate("A", "USD") }
        val viewModel = AiChatViewModel(repository, session)
        val store = ViewModelStore().apply { put("ai", viewModel) }
        try {
            viewModel.updateText("question")
            viewModel.sendMessage("question")
            viewModel.sendMessage("question")
            runCurrent()
            assertEquals(1, repository.requests.size)
            assertEquals("question", viewModel.textState)
            assertNotNull(viewModel.errorResId.value)
            assertFalse(viewModel.isLoading)
            repository.action = {}
            viewModel.sendMessage("question")
            runCurrent()
            assertEquals(repository.requests.first(), repository.requests.last())
            assertEquals("", viewModel.textState)
            assertNull(viewModel.errorResId.value)
        } finally { store.clear() }
    }

    @Test fun failureDoesNotOverwriteANewerDraft() = runTest(main.dispatcher) {
        val failure = CompletableDeferred<Unit>()
        val repository = Repository().apply { action = { failure.await(); throw AiException.Unavailable() } }
        val session = AccountSession().apply { activate("A", "USD") }
        val viewModel = AiChatViewModel(repository, session)
        val store = ViewModelStore().apply { put("ai", viewModel) }
        try {
            viewModel.updateText("sent question")
            viewModel.sendMessage("sent question")
            runCurrent()
            viewModel.updateText("new draft")
            failure.complete(Unit)
            runCurrent()
            assertEquals("new draft", viewModel.textState)
        } finally { store.clear() }
    }

    @Test fun timeoutAndAccessFailuresRestoreTheDraftWithDifferentResourceMessages() = runTest(main.dispatcher) {
        val failures = listOf(
            AiException.TimedOut() to R.string.ai_request_timed_out,
            AiException.AccessVerification() to R.string.ai_access_verification_failed,
            AiException.NetworkUnavailable() to R.string.ai_network_unavailable,
            AiException.ServiceUnavailable() to R.string.ai_service_unavailable
        )
        for ((failure, message) in failures) {
            val repository = Repository().apply { action = { throw failure } }
            val session = AccountSession().apply { activate("A", "USD") }
            val viewModel = AiChatViewModel(repository, session)
            val store = ViewModelStore().apply { put("ai", viewModel) }
            try {
                viewModel.updateText("question")
                viewModel.sendMessage("question")
                runCurrent()
                assertFalse(viewModel.isLoading)
                assertEquals("question", viewModel.textState)
                assertEquals(message, viewModel.errorResId.value)
            } finally { store.clear() }
        }
    }

    @Test fun accountChangeClearsOldDraftRetryAndPendingAutoPrompt() = runTest(main.dispatcher) {
        val repository = Repository().apply { action = { throw AiException.Unavailable() } }
        val session = AccountSession().apply { activate("A", "USD") }
        val viewModel = AiChatViewModel(repository, session)
        val store = ViewModelStore().apply { put("ai", viewModel) }
        try {
            viewModel.sendMessage("question")
            runCurrent()
            viewModel.setPendingPrompt("private prompt")
            session.activate("B", "USD")
            runCurrent()
            assertEquals("", viewModel.textState)
            assertNull(viewModel.errorResId.value)
            viewModel.sendPendingPrompt()
            assertEquals(1, repository.requests.size)
            repository.action = {}
            viewModel.sendMessage("question")
            runCurrent()
            assertEquals("B", repository.requests.last().ownerId)
            assertNotEquals(repository.requests.first().id, repository.requests.last().id)
        } finally { store.clear() }
    }
}
