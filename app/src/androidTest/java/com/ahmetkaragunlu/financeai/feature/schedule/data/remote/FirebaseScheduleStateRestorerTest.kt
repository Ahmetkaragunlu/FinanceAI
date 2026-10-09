package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class FirebaseScheduleStateRestorerTest {
    private class Fixture {
        val functions = mock(FirebaseFunctions::class.java)
        val auth = mock(FirebaseAuth::class.java)
        val callable = mock(HttpsCallableReference::class.java)
        val requests = mutableListOf<Map<String, Any?>>()
        var owner: String? = "A"
        var respond: (Int) -> Task<HttpsCallableResult> = { response(mapOf("nextCursor" to null)) }
        val restorer = FirebaseScheduleStateRestorer(functions, auth)

        init {
            val user = mock(FirebaseUser::class.java)
            `when`(user.uid).thenAnswer { owner }
            `when`(auth.currentUser).thenAnswer { if (owner == null) null else user }
            `when`(functions.getHttpsCallable("restoreScheduleState")).thenReturn(callable)
            doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                requests += invocation.getArgument<Map<String, Any?>>(0)
                respond(requests.size)
            }.`when`(callable).call(any())
        }

        fun response(data: Any?): Task<HttpsCallableResult> {
            val result = mock(HttpsCallableResult::class.java)
            doReturn(data).`when`(result).getData()
            return Tasks.forResult(result)
        }
    }

    @Test fun restoreKeepsTheExistingCallableTokenAndBoundedCursorSequence(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { page -> f.response(mapOf("nextCursor" to if (page == 1) "p50" else null)) }
        f.restorer("A", "synthetic-device")
        assertEquals(listOf(mapOf("deviceToken" to "synthetic-device", "cursor" to null),
            mapOf("deviceToken" to "synthetic-device", "cursor" to "p50")), f.requests)
    }

    @Test fun missingOrChangedAccountStopsRestoreWithoutSendingAnotherPage(): Unit = runBlocking {
        val f = Fixture()
        f.owner = null
        f.restorer("A", "synthetic-device")
        assertEquals(0, f.requests.size)
        verify(f.functions, never()).getHttpsCallable("restoreScheduleState")
        f.owner = "A"
        f.respond = { f.owner = "B"; f.response(mapOf("nextCursor" to "p50")) }
        f.restorer("A", "synthetic-device")
        assertEquals(1, f.requests.size)
    }

    @Test fun malformedResponseAndRepeatedCursorKeepTheExistingTypedFailure(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { f.response("not-a-map") }
        assertThrows(DataAccessException.InvalidRemoteData::class.java) {
            runBlocking { f.restorer("A", "synthetic-device") }
        }
        f.requests.clear()
        f.respond = { f.response(mapOf("nextCursor" to "same")) }
        assertThrows(DataAccessException.InvalidRemoteData::class.java) {
            runBlocking { f.restorer("A", "synthetic-device") }
        }
        assertEquals(2, f.requests.size)
    }

    @Test fun sdkFailureAndCancellationAreNotConvertedToSuccessfulRestore(): Unit = runBlocking {
        val f = Fixture()
        val error = IOException("synthetic offline")
        f.respond = { Tasks.forException(error) }
        assertSame(error, assertThrows(IOException::class.java) {
            runBlocking { f.restorer("A", "synthetic-device") }
        })
        f.requests.clear()
        val gate = TaskCompletionSource<HttpsCallableResult>()
        f.respond = { gate.task }
        val restore = async(start = CoroutineStart.UNDISPATCHED) { f.restorer("A", "synthetic-device") }
        restore.cancelAndJoin()
        assertEquals(1, f.requests.size)
    }
}
