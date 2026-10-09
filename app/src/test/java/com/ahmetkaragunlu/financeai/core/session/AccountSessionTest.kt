package com.ahmetkaragunlu.financeai.core.session

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionTest {
    @Test fun `switching accounts rejects old generation including a return to same account`() = runTest {
        val session = AccountSession()
        session.activate("A", "USD")
        val old = session.requireAccount()
        session.deactivate()
        assertFalse(session.isCurrent(old))
        session.activate("B", "EUR")
        assertFalse(session.isCurrent(old))
        session.deactivate()
        session.activate("A", "USD")
        assertFalse(session.isCurrent(old))
        assertEquals("A", session.requireAccount().ownerId)
    }
    @Test fun `signed out mutation cannot obtain account ownership`() {
        assertThrows(IllegalStateException::class.java) { AccountSession().requireAccount() }
    }

    @Test fun `state transitions work while signed out but mutations still require ownership`() = runTest {
        val session = AccountSession()
        session.withStateLock { session.activate("A", "USD") }
        session.withAccount { assertEquals("A", it.ownerId) }
        session.withStateLock { session.deactivate() }
        var rejected = false
        try { session.withAccount { error("Signed-out mutation entered") } }
        catch (_: IllegalStateException) { rejected = true }
        assertTrue(rejected)
    }

    @Test fun `mutation and state transition share one guard and cancelled waiters do not retain it`() = runTest {
        val session = AccountSession().apply { activate("A", "USD") }
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val mutation = launch { session.withAccount { events += "mutation"; entered.complete(Unit); release.await() } }
        entered.await()
        val transition = launch { session.withStateLock { events += "transition"; session.deactivate() } }
        runCurrent()
        assertEquals(listOf("mutation"), events)
        transition.cancelAndJoin()
        release.complete(Unit)
        mutation.join()
        session.withStateLock { events += "next"; session.deactivate() }
        assertEquals(listOf("mutation", "next"), events)
    }

    @Test fun `late emission from the former owner cannot leak before the latest source switches`() = runTest {
        val session = AccountSession().apply { activate("A", "USD") }
        val values = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            session.observe("empty") { account -> flow {
                emit(account.ownerId)
                if (account.ownerId == "A") { session.activate("B", "EUR"); emit("late-A") }
            } }.collect { values += it }
        }
        runCurrent()
        assertFalse(values.contains("late-A"))
        assertEquals("B", values.last())
        session.deactivate()
        runCurrent()
        assertEquals("empty", values.last())
    }
}
