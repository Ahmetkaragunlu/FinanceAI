package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.media.local.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoStorageManager
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

/** Wire compatibility only; upload orchestration is tested separately with its owning rules. */
class PhotoUploadWorkerInputTest {
    private fun worker(f: AccountDatabaseFixture, kind: String?, firestore: FirebaseFirestore,
        storage: PhotoStorageManager): PhotoUploadWorker {
        val sessions = mock(SessionCoordinator::class.java)
        `when`(sessions.session).thenReturn(f.session)
        val files = mock(PhotoLocalStore::class.java)
        val input = workDataOf(AccountWork.OWNER_ID to "A", PhotoUploadWorker.KEY_FIRESTORE_ID to "record",
            PhotoUploadWorker.KEY_LOCAL_PATH to "/test/IMG_wire.jpg")
        val data = Data.Builder().putAll(input).apply {
            if (kind != null) putString(PhotoUploadWorker.KEY_COLLECTION_TYPE, kind)
        }.build()
        val factory = object : WorkerFactory() {
            override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                PhotoUploadWorker(context, parameters, storage, sessions, f.database, firestore, files)
        }
        return TestListenableWorkerBuilder<PhotoUploadWorker>(f.context).setInputData(data)
            .setWorkerFactory(factory).build()
    }

    @Test fun oldMissingKindDefaultsToTransactionsAndScheduledKeepsItsOwnDurableWireValue() = runBlocking {
        for ((kind, expected) in listOf(null to "transactions", "scheduled" to "scheduled")) {
            AccountDatabaseFixture().use { f ->
                f.activate()
                for (collection in listOf("transactions", "scheduled")) {
                    f.database.photoOperationDao().insert(PhotoOperation("A", collection, "record", "/test/IMG_wire.jpg", "IMG_wire"))
                }
                val firestore = mock(FirebaseFirestore::class.java)
                val storage = mock(PhotoStorageManager::class.java)
                // No matching local record: only the selected durable operation is retired.
                assertEquals(ListenableWorker.Result.success(), worker(f, kind, firestore, storage).doWork())
                assertEquals(listOf(if (expected == "transactions") "scheduled" else "transactions"),
                    f.database.photoOperationDao().forAccount("A").map { it.collection })
                verifyNoInteractions(firestore, storage)
            }
        }
    }

    @Test fun unsupportedKindFailsBeforeSessionOrRemoteWorkWithoutAcknowledgingAnOperation() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            f.database.photoOperationDao().insert(PhotoOperation("A", "transactions", "record", "/test/IMG_wire.jpg", "IMG_wire"))
            val firestore = mock(FirebaseFirestore::class.java)
            val storage = mock(PhotoStorageManager::class.java)
            for (kind in listOf("", "TRANSACTION", "scheduled_transactions")) {
                assertEquals(ListenableWorker.Result.failure(), worker(f, kind, firestore, storage).doWork())
            }
            assertEquals(1, f.database.photoOperationDao().forAccount("A").size)
            verifyNoInteractions(firestore, storage)
        }
    }
}
