package com.ahmetkaragunlu.financeai.feature.aichat.data.repository

import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiConversationStore
import com.ahmetkaragunlu.financeai.feature.aichat.data.report.FinancialSnapshotSource
import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import com.ahmetkaragunlu.financeai.feature.aichat.domain.generation.AiTextGenerator
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiRequest
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.FinancialSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiRepositoryImplTest {
    private class Store : AiConversationStore {
        data class Row(val owner: String, val text: String, val isAi: Boolean)
        val rows = linkedMapOf<String, Row>()
        override fun observe(): Flow<List<AiMessage>> = flowOf(emptyList())
        override suspend fun contains(account: ActiveAccount, messageId: String) = rows[messageId]?.owner == account.ownerId
        override suspend fun save(account: ActiveAccount, messageId: String, text: String, isAi: Boolean) {
            rows.putIfAbsent(messageId, Row(account.ownerId, text, isAi))
        }
    }
    private class Generator(var action: suspend () -> String) : AiTextGenerator {
        var calls = 0
        override suspend fun generate(question: String, snapshot: FinancialSnapshot): String { calls++; return action() }
    }
    private class Fixture {
        val session = AccountSession().apply { activate("A", "USD") }
        val store = Store()
        val generator = Generator { "answer" }
        val snapshots = object : FinancialSnapshotSource {
            override suspend fun read(account: ActiveAccount) = FinancialSnapshot("USD", 1, 0, 2, emptyList(), emptyList())
        }
        val repository = AiRepositoryImpl(generator, store, snapshots, session)
        val request = AiRequest("A", "request-1", "question")
    }

    @Test fun failureKeepsOneUserRowAndRetryCreatesOneReply() = runTest {
        val f = Fixture()
        f.generator.action = { throw AiException.Unavailable() }
        try { f.repository.sendMessage(f.request); fail("Expected provider failure") }
        catch (_: AiException.Unavailable) { }
        assertEquals(1, f.store.rows.size)
        assertFalse(f.store.rows.values.single().isAi)
        f.generator.action = { "answer" }
        f.repository.sendMessage(f.request)
        f.repository.sendMessage(f.request)
        assertEquals(setOf("request-1", "request-1_reply"), f.store.rows.keys)
        assertEquals(2, f.generator.calls)
    }

    @Test fun accountChangeRejectsLateReplyAndForeignRetry() = runTest {
        val f = Fixture()
        f.generator.action = { f.session.activate("B", "USD"); "late answer" }
        try { f.repository.sendMessage(f.request); fail("Late account result was accepted") }
        catch (_: CancellationException) { }
        assertEquals(1, f.store.rows.size)
        try { f.repository.sendMessage(f.request); fail("Foreign retry was accepted") }
        catch (_: CancellationException) { }
        assertEquals(1, f.generator.calls)
        assertEquals("A", f.store.rows.values.single().owner)
    }

    @Test fun sameAccountNewGenerationAlsoRejectsLateResult() = runTest {
        val f = Fixture()
        f.generator.action = { f.session.deactivate(); f.session.activate("A", "USD"); "late answer" }
        try { f.repository.sendMessage(f.request); fail("Old generation accepted") }
        catch (_: CancellationException) { }
        assertEquals(1, f.store.rows.size)
    }

    @Test fun concurrentRetriesShareOneModelCallAndReply() = runTest {
        val f = Fixture()
        val reply = CompletableDeferred<String>()
        f.generator.action = { reply.await() }
        val first = async { f.repository.sendMessage(f.request) }
        val second = async { f.repository.sendMessage(f.request) }
        runCurrent()
        assertEquals(1, f.generator.calls)
        reply.complete("answer")
        first.await(); second.await()
        assertEquals(1, f.generator.calls)
        assertEquals(2, f.store.rows.size)
    }

    @Test fun cancellationNeverPersistsAnErrorAsAnAiMessage() = runTest {
        val f = Fixture()
        f.generator.action = { throw CancellationException("cancel") }
        try { f.repository.sendMessage(f.request); fail("Cancellation lost") }
        catch (_: CancellationException) { }
        assertEquals(1, f.store.rows.size)
        assertEquals("question", f.store.rows.values.single().text)
    }
}
