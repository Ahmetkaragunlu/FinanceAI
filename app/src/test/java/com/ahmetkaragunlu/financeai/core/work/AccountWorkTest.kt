package com.ahmetkaragunlu.financeai.core.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccountWorkTest {
    @Test fun accountCancellationKeepsThePersistedTagAndSeparatesOwners() {
        assertEquals("account_owner-with_underscores", AccountWork.tag("owner-with_underscores"))
        assertNotEquals(AccountWork.tag("A"), AccountWork.tag("B"))
    }
}
