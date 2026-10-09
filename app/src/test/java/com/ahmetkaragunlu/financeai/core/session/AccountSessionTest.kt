package com.ahmetkaragunlu.financeai.core.session

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

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
}
