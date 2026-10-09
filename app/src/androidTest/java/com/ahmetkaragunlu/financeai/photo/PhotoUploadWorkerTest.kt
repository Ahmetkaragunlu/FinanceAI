package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.local.PhotoFiles
import com.ahmetkaragunlu.financeai.core.media.local.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoStorageManager
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduleCommand
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import com.google.firebase.storage.StorageException
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock

class PhotoUploadWorkerTest {
    private class Fixture(val kind: PhotoRecordType = PhotoRecordType.TRANSACTION, val record: String = "receipt") : AutoCloseable {
        val local = AccountDatabaseFixture()
        val owner = "upload-test-${UUID.randomUUID()}"
        val folder = File(local.context.filesDir, "${PhotoFiles.DIRECTORY}/$owner").apply { mkdirs() }
        val file = File(folder, "IMG_current.jpg").apply { writeText("synthetic receipt") }
        val version = "IMG_current"
        val url = "https://example.test/uploaded/$version"
        val sessions = mock(SessionCoordinator::class.java)
        val storage = mock(PhotoStorageManager::class.java)
        val firestore = mock(FirebaseFirestore::class.java)
        val collection = mock(CollectionReference::class.java)
        val reference = mock(DocumentReference::class.java)
        val snapshot = mock(DocumentSnapshot::class.java)
        val transaction = mock(Transaction::class.java)
        val files = PhotoLocalStore(local.context, local.database, Dispatchers.IO, local.clock)
        val readStarted = CompletableDeferred<Unit>()
        val remote = mutableMapOf<String, Any?>("userId" to owner, "deleted" to false,
            "photoIntent" to version, "photoVersion" to null, "revision" to 5L)
        val attached = mutableListOf<Map<String, Any?>>()
        val deletedUrls = mutableListOf<String>()
        var exists = true
        var read: Task<DocumentSnapshot> = Tasks.forResult(snapshot)
        var uploadCalls = 0
        var beforeTransaction: () -> Unit = {}

        init {
            `when`(sessions.session).thenReturn(local.session)
            `when`(firestore.collection(if (kind == PhotoRecordType.SCHEDULED) "scheduled_transactions" else "transactions")).thenReturn(collection)
            `when`(collection.document(record)).thenReturn(reference)
            doAnswer { readStarted.complete(Unit); read }.`when`(reference).get(Source.SERVER)
            `when`(snapshot.exists()).thenAnswer { exists }
            for (key in listOf("userId", "photoIntent", "photoVersion")) {
                `when`(snapshot.getString(key)).thenAnswer { remote[key] as? String }
            }
            `when`(snapshot.getBoolean("deleted")).thenAnswer { remote["deleted"] as? Boolean }
            `when`(snapshot.getLong("revision")).thenAnswer { (remote["revision"] as? Number)?.toLong() }
            `when`(transaction.get(reference)).thenReturn(snapshot)
            doAnswer { invocation ->
                attached += invocation.getArgument<Map<String, Any?>>(1)
                transaction
            }.`when`(transaction).update(eq(reference), any<Map<String, Any?>>())
            doAnswer { invocation ->
                beforeTransaction()
                val action = invocation.getArgument<Transaction.Function<Boolean>>(0)
                Tasks.forResult(action.apply(transaction))
            }.`when`(firestore).runTransaction(any<Transaction.Function<Boolean>>())
        }

        suspend fun prepare() {
            local.activate(owner)
            setLocalPath(file.path)
            local.database.photoOperationDao().insert(PhotoOperation(owner, kind.wireValue, record, file.path, version))
            setUpload()
            doAnswer { deletedUrls += url; Unit }.`when`(storage).deletePhoto(url, owner)
        }

        suspend fun setUpload(action: () -> String = { url }) {
            doAnswer { uploadCalls++; action() }.`when`(storage).uploadPhoto(file.path, record, kind, owner, version)
        }

        suspend fun setLocalPath(path: String?) {
            if (kind == PhotoRecordType.SCHEDULED) {
                val previous = local.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(record)
                local.database.scheduledTransactionDao().insertScheduledTransaction(ScheduledTransactionEntity(
                    id = previous?.id ?: 0, ownerId = owner, firestoreId = record, currencyCode = "USD", amountMinor = 2550,
                    type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = null, scheduledDate = 100, photoUri = path))
            } else {
                val previous = local.database.transactionDao().getTransactionByFirestoreId(record)
                local.database.transactionDao().insertTransaction(TransactionEntity(id = previous?.id ?: 0,
                    ownerId = owner, firestoreId = record, currencyCode = "USD", amountMinor = 2550,
                    transaction = TransactionType.EXPENSE, category = CategoryType.FOOD, date = 100, photoUri = path))
            }
        }

        suspend fun operation() = local.database.photoOperationDao().forAccount(owner).singleOrNull()

        fun worker(): PhotoUploadWorker {
            val factory = object : WorkerFactory() {
                override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                    PhotoUploadWorker(context, parameters, storage, sessions, local.database, firestore, files)
            }
            return TestListenableWorkerBuilder<PhotoUploadWorker>(local.context).setWorkerFactory(factory).setInputData(workDataOf(
                "account_owner_id" to owner, "local_path" to file.path, "firestore_id" to record,
                "collection_type" to kind.wireValue, "version" to version)).build()
        }

        override fun close() { local.close(); folder.deleteRecursively() }
    }

    @Test fun successfulAttachmentUsesOnlyTheCorrectVersionMetadataAndKeepsAReferencedLocalFile(): Unit = runBlocking {
        for (kind in PhotoRecordType.entries) Fixture(kind).use { f ->
            f.prepare()
            assertEquals(ListenableWorker.Result.success(), f.worker().doWork())
            assertEquals(mapOf("photoStorageUrl" to f.url, "photoRemoved" to false,
                "photoIntent" to f.version, "photoVersion" to f.version, "revision" to 6L), f.attached.single())
            assertEquals(1, f.uploadCalls)
            assertNull(f.operation())
            assertTrue(f.file.isFile)
            assertTrue(f.deletedUrls.isEmpty())
        }
    }

    @Test fun oldAccountWorkDoesNotUploadRetireOrDeleteItsRetainedIntent(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare()
            f.local.activate("another-synthetic-account")
            assertEquals(ListenableWorker.Result.success(), f.worker().doWork())
            assertNotNull(f.operation())
            assertTrue(f.file.isFile)
            assertEquals(0, f.uploadCalls)
        }
    }

    @Test fun staleLocalPathRetiresOnlyItsOperationAndHonorsOtherPhotoReferences(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare()
            val next = File(f.folder, "IMG_next.jpg").apply { writeText("next") }
            f.setLocalPath(next.path)
            f.local.database.photoOperationDao().insert(PhotoOperation(f.owner, "scheduled", "other-plan", f.file.path, "shared"))
            assertEquals(ListenableWorker.Result.success(), f.worker().doWork())
            assertEquals(listOf("other-plan"), f.local.database.photoOperationDao().forAccount(f.owner).map { it.remoteId })
            assertTrue(f.file.isFile)
            assertTrue(next.isFile)
            assertEquals(0, f.uploadCalls)
        }
    }

    @Test fun missingFileIsARecordedFailureRatherThanAnInfiniteRetry(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare()
            check(f.file.delete())
            assertEquals(ListenableWorker.Result.failure(), f.worker().doWork())
            assertEquals("missing_file", f.operation()?.failure)
            assertEquals(0, f.uploadCalls)
        }
    }

    @Test fun missingRemoteUpsertRetriesWhilePermanentOrAbsentIntentRecordsFailure(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare(); f.exists = false
            val row = SyncRecord(f.owner, "transactions", f.record,
                pendingPayload = SyncPayload.encode(mapOf("amountMinor" to 2550L)), mutationId = "pending")
            f.local.database.syncRecordDao().save(row)
            assertEquals(ListenableWorker.Result.retry(), f.worker().doWork())
            assertNull(f.operation()?.failure)
            f.local.database.syncRecordDao().save(row.copy(permanentFailure = true))
            assertEquals(ListenableWorker.Result.failure(), f.worker().doWork())
            assertEquals("remote_record_unavailable", f.operation()?.failure)
            assertTrue(f.file.isFile)
        }
        Fixture().use { f ->
            f.prepare(); f.exists = false
            assertEquals(ListenableWorker.Result.failure(), f.worker().doWork())
            assertEquals("remote_record_unavailable", f.operation()?.failure)
        }
    }

    @Test fun missingCompletedRecordWaitsForItsDurableCompletionCommand(): Unit = runBlocking {
        Fixture(record = "completed_plan").use { f ->
            f.prepare(); f.exists = false
            val command = ScheduleCommand("operation", f.owner, "plan", 100, "complete", 100)
            f.local.database.scheduleCommandDao().insert(command)
            assertEquals(ListenableWorker.Result.retry(), f.worker().doWork())
            assertNull(f.operation()?.failure)
            f.local.database.scheduleCommandDao().fail(command.operationId, "conflict")
            assertEquals(ListenableWorker.Result.failure(), f.worker().doWork())
            assertEquals("remote_record_unavailable", f.operation()?.failure)
        }
    }

    @Test fun alreadyAttachedDeletedAndForeignRemoteRecordsNeverReceiveAnotherUpload(): Unit = runBlocking {
        for (state in listOf("attached", "deleted", "foreign")) Fixture().use { f ->
            f.prepare()
            when (state) {
                "attached" -> f.remote["photoVersion"] = f.version
                "deleted" -> f.remote["deleted"] = true
                else -> f.remote["userId"] = "other-owner"
            }
            assertEquals(ListenableWorker.Result.success(), f.worker().doWork())
            assertNull(f.operation())
            assertEquals(0, f.uploadCalls)
            assertTrue(f.file.isFile)
            assertTrue(f.attached.isEmpty())
        }
    }

    @Test fun differentIntentBeforeOrAfterUploadRetainsRetryAndCannotOverwriteNewerMetadata(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare(); f.remote["photoIntent"] = "newer"
            assertEquals(ListenableWorker.Result.retry(), f.worker().doWork())
            assertEquals(0, f.uploadCalls)
            assertNotNull(f.operation())
            f.remote["photoIntent"] = f.version
            f.setUpload { f.remote["photoIntent"] = "newer"; f.url }
            assertEquals(ListenableWorker.Result.retry(), f.worker().doWork())
            assertTrue(f.attached.isEmpty())
            assertEquals(listOf(f.url), f.deletedUrls)
            assertNull(f.operation()?.failure)
            assertTrue(f.file.isFile)
        }
    }

    @Test fun localReplacementDuringUploadDeletesOnlyTheUnusedRemoteVersion(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare()
            val next = File(f.folder, "IMG_next.jpg").apply { writeText("next") }
            f.setUpload { runBlocking { f.setLocalPath(next.path) }; f.url }
            assertEquals(ListenableWorker.Result.success(), f.worker().doWork())
            assertEquals(listOf(f.url), f.deletedUrls)
            assertNull(f.operation())
            assertTrue(f.attached.isEmpty())
            assertFalse(f.file.exists())
            assertTrue(next.isFile)
        }
    }

    @Test fun accountSwitchAfterUploadCannotAttachOrAcknowledgeAnOldAccountsPhoto(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare()
            f.setUpload { runBlocking { f.local.activate("another-synthetic-account") }; f.url }
            assertEquals(ListenableWorker.Result.success(), f.worker().doWork())
            assertTrue(f.attached.isEmpty())
            assertTrue(f.deletedUrls.isEmpty())
            assertNotNull(f.operation())
            assertTrue(f.file.isFile)
        }
    }

    @Test fun anotherSuccessfulAttachmentOfTheSameVersionDoesNotRewriteOrDeleteIt(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare()
            f.setUpload { f.remote["photoVersion"] = f.version; f.url }
            assertEquals(ListenableWorker.Result.success(), f.worker().doWork())
            assertTrue(f.attached.isEmpty())
            assertTrue(f.deletedUrls.isEmpty())
            assertNull(f.operation())
            assertTrue(f.file.isFile)
        }
    }

    @Test fun accountChangeAtTheTransactionBoundaryCannotCommitOldPhotoMetadata(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare()
            f.beforeTransaction = { runBlocking { f.local.activate("another-synthetic-account") } }
            assertEquals(ListenableWorker.Result.retry(), f.worker().doWork())
            assertTrue(f.attached.isEmpty())
            assertTrue(f.deletedUrls.isEmpty())
            assertNull(f.operation()?.failure)
            assertTrue(f.file.isFile)
        }
    }

    @Test fun firestoreFailurePoliciesKeepFailedPreconditionRetryableAndAuthorizationPermanent(): Unit = runBlocking {
        for ((code, permanent) in listOf(FirebaseFirestoreException.Code.FAILED_PRECONDITION to false,
            FirebaseFirestoreException.Code.UNAVAILABLE to false, FirebaseFirestoreException.Code.PERMISSION_DENIED to true,
            FirebaseFirestoreException.Code.INVALID_ARGUMENT to true)) Fixture().use { f ->
            f.prepare()
            f.read = Tasks.forException(FirebaseFirestoreException("synthetic failure", code))
            assertEquals(if (permanent) ListenableWorker.Result.failure() else ListenableWorker.Result.retry(), f.worker().doWork())
            assertEquals(permanent, f.operation()?.failure != null)
            assertTrue(f.file.isFile)
        }
    }

    @Test fun storageAuthorizationAndQuotaRemainPermanentButNetworkErrorsRetainRetry(): Unit = runBlocking {
        for (code in listOf(StorageException.ERROR_NOT_AUTHORIZED, StorageException.ERROR_QUOTA_EXCEEDED)) Fixture().use { f ->
            f.prepare()
            val error = mock(StorageException::class.java)
            `when`(error.errorCode).thenReturn(code)
            f.setUpload { throw error }
            assertEquals(ListenableWorker.Result.failure(), f.worker().doWork())
            assertNotNull(f.operation()?.failure)
            assertTrue(f.file.isFile)
        }
        Fixture().use { f ->
            f.prepare(); f.setUpload { throw IOException("synthetic offline") }
            assertEquals(ListenableWorker.Result.retry(), f.worker().doWork())
            assertNull(f.operation()?.failure)
            assertTrue(f.file.isFile)
        }
    }

    @Test fun cancellationDuringRemoteLookupKeepsPendingIntentAndNeverBecomesRetryOrFailure(): Unit = runBlocking {
        Fixture().use { f ->
            f.prepare()
            val gate = TaskCompletionSource<DocumentSnapshot>()
            f.read = gate.task
            val job = async { f.worker().doWork() }
            withTimeout(5_000) { f.readStarted.await() }
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertNull(f.operation()?.failure)
            assertTrue(f.file.isFile)
            assertTrue(f.attached.isEmpty())
            assertEquals(0, f.uploadCalls)
        }
    }
}
