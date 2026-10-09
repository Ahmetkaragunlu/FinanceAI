package com.ahmetkaragunlu.financeai.fcm.work

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.fcm.testing.TokenManagerFixture
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.RestoreScheduleState
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class TokenRegistrationWorkerTest {
    private fun worker(f: TokenManagerFixture, input: Data, restores: RestoreScheduleState): TokenRegistrationWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                TokenRegistrationWorker(context, parameters, f.manager, f.auth, restores)
        }
        return TestListenableWorkerBuilder<TokenRegistrationWorker>(f.local.context)
            .setInputData(input).setWorkerFactory(factory).build()
    }

    private fun noRestore() = object : RestoreScheduleState {
        override suspend fun invoke(ownerId: String, deviceToken: String) = error("Unexpected schedule restore")
    }

    @Test fun persistedFetchCurrentInputRestoresOnlyAfterTheFetchedTokenIsFlushed(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            `when`(f.messaging.token).thenReturn(Tasks.forResult("synthetic-current-device"))
            var called = 0
            val restores = object : RestoreScheduleState {
                override suspend fun invoke(ownerId: String, deviceToken: String) {
                    assertEquals("A", ownerId)
                    assertEquals("synthetic-current-device", deviceToken)
                    assertTrue(f.local.database.tokenOperationDao().pending("A").isEmpty())
                    assertTrue(checkNotNull(f.local.database.tokenOperationDao().latestRegistered("A")).acknowledged)
                    called++
                }
            }
            assertEquals(ListenableWorker.Result.success(), worker(f,
                workDataOf("account_owner_id" to "A", "fetch_current" to true), restores).doWork())
            assertEquals(1, called)
        }
    }

    @Test fun normalSuppliedTokenRegistersWithoutFetchingOrRestoringScheduleState(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            assertEquals(ListenableWorker.Result.success(), worker(f,
                workDataOf("account_owner_id" to "A", "fcm_token" to "synthetic-supplied-device"), noRestore()).doWork())
            assertEquals("synthetic-supplied-device", f.manager.registeredDeviceToken("A"))
            assertTrue(f.local.database.tokenOperationDao().pending("A").isEmpty())
            verify(f.messaging, never()).token
        }
    }

    @Test fun missingOwnerKeepsCurrentAccountFallbackButUnverifiedAndStaleAccountsCannotRestore(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            f.signIn("A", verified = false)
            `when`(f.messaging.token).thenReturn(Tasks.forResult("synthetic-unverified-device"))
            assertEquals(ListenableWorker.Result.success(), worker(f,
                workDataOf("fetch_current" to true), noRestore()).doWork())
            assertTrue(f.local.database.tokenOperationDao().pending("A").isEmpty())
            f.signIn("B")
            assertEquals(ListenableWorker.Result.success(), worker(f,
                workDataOf("account_owner_id" to "A", "fetch_current" to true), noRestore()).doWork())
            assertTrue(f.local.database.tokenOperationDao().pending("B").isEmpty())
        }
    }

    @Test fun flushAndRestoreFailuresKeepRetrySemanticsAndDoNotLoseRegistrationIntent(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            `when`(f.messaging.token).thenReturn(Tasks.forResult("synthetic-current-device"))
            f.write = Tasks.forException(IOException("synthetic offline"))
            val input = workDataOf("account_owner_id" to "A", "fetch_current" to true)
            assertEquals(ListenableWorker.Result.retry(), worker(f, input, noRestore()).doWork())
            assertEquals(1, f.local.database.tokenOperationDao().pending("A").size)
            f.write = Tasks.forResult(null)
            val restores = object : RestoreScheduleState {
                override suspend fun invoke(ownerId: String, deviceToken: String) {
                    throw IOException("synthetic restore offline")
                }
            }
            assertEquals(ListenableWorker.Result.retry(), worker(f, input, restores).doWork())
            assertTrue(f.local.database.tokenOperationDao().pending("A").isEmpty())
        }
    }

    @Test fun cancellationOfFetchIsNotConvertedToRetryOrScheduleRestore(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            val token = TaskCompletionSource<String>()
            `when`(f.messaging.token).thenReturn(token.task)
            val request = async(start = CoroutineStart.UNDISPATCHED) {
                worker(f, workDataOf("account_owner_id" to "A", "fetch_current" to true), noRestore()).doWork()
            }
            request.cancelAndJoin()
            assertTrue(request.isCancelled)
            assertTrue(f.local.database.tokenOperationDao().pending("A").isEmpty())
        }
    }
}
