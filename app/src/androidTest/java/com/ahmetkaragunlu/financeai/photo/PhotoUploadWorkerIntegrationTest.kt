package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.local.PhotoFiles
import com.ahmetkaragunlu.financeai.core.media.local.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoStorageManager
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.sync.testing.EmulatorAccountFixture
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.google.firebase.firestore.Source
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

/** Real SDK transactions and Room, with only upload/delete controlled; never a live bucket. */
class PhotoUploadWorkerIntegrationTest {
    @Test fun serverTransactionAttachesOnlyPhotoMetadataWithoutChangingTheFinancialRecord(): Unit = runBlocking {
        scenario(changeIntent = false)
    }

    @Test fun serverIntentChangeDuringUploadPreventsAStaleAttachmentAndRetainsRetry(): Unit = runBlocking {
        scenario(changeIntent = true)
    }

    private suspend fun scenario(changeIntent: Boolean) {
        val f = EmulatorAccountFixture()
        val owner = "photo-integration-${UUID.randomUUID()}"
        val folder = File(f.local.context.filesDir, "${PhotoFiles.DIRECTORY}/$owner").apply { mkdirs() }
        try {
            withTimeout(25_000) {
                f.activate(owner)
                val file = File(folder, "IMG_current.jpg").apply { writeText("synthetic image") }
                val record = "receipt-${UUID.randomUUID()}"
                val version = file.nameWithoutExtension
                val url = "https://example.test/synthetic/$version"
                f.local.database.transactionDao().insertTransaction(TransactionEntity(ownerId = owner, firestoreId = record,
                    currencyCode = "USD", amountMinor = 1000, transaction = TransactionType.EXPENSE,
                    category = CategoryType.FOOD, date = 100, photoUri = file.path))
                f.local.database.photoOperationDao().insert(PhotoOperation(owner, "transactions", record, file.path, version))
                val reference = f.firestore.collection("transactions").document(record)
                reference.set(mapOf("userId" to owner, "deleted" to false, "amountMinor" to 1000L,
                    "currencyCode" to "USD", "transaction" to "EXPENSE", "category" to "FOOD", "date" to 100L,
                    "photoIntent" to version, "photoVersion" to null, "revision" to 9L)).await()
                val storage = mock(PhotoStorageManager::class.java)
                val sessions = mock(SessionCoordinator::class.java)
                `when`(sessions.session).thenReturn(f.local.session)
                val deleted = mutableListOf<String>()
                doAnswer {
                    if (changeIntent) runBlocking {
                        reference.update(mapOf("photoIntent" to "newer", "revision" to 10L)).await()
                    }
                    url
                }.`when`(storage).uploadPhoto(file.path, record, PhotoRecordType.TRANSACTION, owner, version)
                doAnswer { deleted += url; Unit }.`when`(storage).deletePhoto(url, owner)
                val files = PhotoLocalStore(f.local.context, f.local.database, Dispatchers.IO, f.local.clock)
                val factory = object : WorkerFactory() {
                    override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                        PhotoUploadWorker(context, parameters, storage, sessions, f.local.database, f.firestore, files)
                }
                val worker = TestListenableWorkerBuilder<PhotoUploadWorker>(f.local.context).setWorkerFactory(factory)
                    .setInputData(workDataOf("account_owner_id" to owner, "local_path" to file.path,
                        "firestore_id" to record, "collection_type" to "transactions", "version" to version)).build()
                assertEquals(if (changeIntent) ListenableWorker.Result.retry() else ListenableWorker.Result.success(), worker.doWork())
                val remote = reference.get(Source.SERVER).await()
                assertEquals(1000L, remote.getLong("amountMinor"))
                assertEquals(100L, remote.getLong("date"))
                assertEquals(10L, remote.getLong("revision"))
                assertTrue(file.isFile)
                val operations = f.local.database.photoOperationDao().forAccount(owner)
                if (changeIntent) {
                    assertNull(remote.getString("photoStorageUrl"))
                    assertNull(remote.getString("photoVersion"))
                    assertEquals("newer", remote.getString("photoIntent"))
                    assertEquals(listOf(url), deleted)
                    assertNull(operations.single().failure)
                } else {
                    assertEquals(url, remote.getString("photoStorageUrl"))
                    assertEquals(version, remote.getString("photoVersion"))
                    assertEquals(version, remote.getString("photoIntent"))
                    assertTrue(operations.isEmpty())
                    assertTrue(deleted.isEmpty())
                }
            }
        } finally {
            f.close()
            folder.deleteRecursively()
        }
    }
}
