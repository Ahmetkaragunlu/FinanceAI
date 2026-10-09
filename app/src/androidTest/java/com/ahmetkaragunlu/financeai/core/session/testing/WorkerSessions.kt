package com.ahmetkaragunlu.financeai.core.session.testing

import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

/** Shared readiness double for Worker tests; actual preparation is covered by SDK/session tests. */
fun workerSessions(session: AccountSession): SessionCoordinator = mock(SessionCoordinator::class.java).also { coordinator ->
    `when`(coordinator.session).thenReturn(session)
    runBlocking {
        doAnswer { call -> session.account.value?.takeIf { it.ownerId == call.getArgument<String>(0) } }
            .`when`(coordinator).activeAccountFor(anyString())
    }
}
