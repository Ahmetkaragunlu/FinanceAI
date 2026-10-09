package com.ahmetkaragunlu.financeai.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.session.testing.workerSessions
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class AccountSyncWorkerTest {
    private fun worker(context: Context, input: Data, sessions: SessionCoordinator, engine: AccountSyncEngine): AccountSyncWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                AccountSyncWorker(context, parameters, sessions, engine)
        }
        return TestListenableWorkerBuilder<AccountSyncWorker>(context).setWorkerFactory(factory).setInputData(input).build()
    }

    @Test fun missingOwnerFailsAndAnotherAccountIsANoOpWithoutSync(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessions = workerSessions(AccountSession().apply { activate("A", "USD") })
        val engine = mock(AccountSyncEngine::class.java)
        assertEquals(ListenableWorker.Result.failure(), worker(context, Data.EMPTY, sessions, engine).doWork())
        assertEquals(ListenableWorker.Result.success(), worker(context, workDataOf("account_owner_id" to "B"), sessions, engine).doWork())
        verifyNoInteractions(engine)
    }

    @Test fun successAndFirestoreFailureSetsKeepTheirExistingResults(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = AccountSession().apply { activate("A", "USD") }
        val sessions = workerSessions(session)
        val input = workDataOf("account_owner_id" to "A")
        val engine = mock(AccountSyncEngine::class.java)
        assertEquals(ListenableWorker.Result.success(), worker(context, input, sessions, engine).doWork())
        verify(engine).synchronize(session.requireAccount())
        for ((code, permanent) in listOf(FirebaseFirestoreException.Code.PERMISSION_DENIED to true,
            FirebaseFirestoreException.Code.INVALID_ARGUMENT to true, FirebaseFirestoreException.Code.FAILED_PRECONDITION to true,
            FirebaseFirestoreException.Code.UNAVAILABLE to false)) {
            doAnswer { throw FirebaseFirestoreException("synthetic failure", code) }.`when`(engine).synchronize(session.requireAccount())
            assertEquals(if (permanent) ListenableWorker.Result.failure() else ListenableWorker.Result.retry(), worker(context, input, sessions, engine).doWork())
        }
        doAnswer { throw IOException("synthetic offline") }.`when`(engine).synchronize(session.requireAccount())
        assertEquals(ListenableWorker.Result.retry(), worker(context, input, sessions, engine).doWork())
    }

    @Test fun cancellationIsNeverConvertedToAWorkerResult(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = AccountSession().apply { activate("A", "USD") }
        val engine = mock(AccountSyncEngine::class.java)
        doAnswer { throw CancellationException("synthetic cancellation") }.`when`(engine).synchronize(session.requireAccount())
        assertThrows(CancellationException::class.java) {
            runBlocking { worker(context, workDataOf("account_owner_id" to "A"), workerSessions(session), engine).doWork() }
        }
    }
}
