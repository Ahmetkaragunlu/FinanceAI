package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.media.local.PhotoFiles
import com.ahmetkaragunlu.financeai.core.media.local.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.media.testing.ReceiptImage
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import com.ahmetkaragunlu.financeai.feature.transaction.testing.RecordingTransactionRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionDetailViewModelTest {
    private lateinit var fixture: AccountDatabaseFixture
    private lateinit var vm: TransactionDetailViewModel
    private val repository = RecordingTransactionRepository()
    private val models = ViewModelStore()
    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val owner = "detail-vm-${UUID.randomUUID()}"
    private val original = Transaction(id = 5, firestoreId = "receipt", amount = 25.50,
        transaction = TransactionType.EXPENSE, category = CategoryType.FOOD, note = "original",
        date = 100, ownerId = owner, currencyCode = "USD")

    @Before fun setup() = runBlocking {
        fixture = AccountDatabaseFixture()
        fixture.activate(owner)
        repository.row.value = original
        vm = create(repository, "detail")
    }

    private suspend fun create(repo: RecordingTransactionRepository, key: String) = withContext(Dispatchers.Main) {
        TransactionDetailViewModel(
            PhotoLocalStore(fixture.context, fixture.database, Dispatchers.IO, fixture.clock),
            fixture.session, repo, PhotoWorkScheduler(fixture.workManager, fixture.session, fixture.database),
            SavedStateHandle(mapOf("transactionId" to 5))
        ).also { models.put(key, it) }
    }

    private suspend fun observe(model: TransactionDetailViewModel = vm) {
        observationScope.launch { model.uiState.collect() }
        withTimeout(5_000) { model.uiState.first { it != TransactionDetailUiState.Loading } }
    }

    @After fun close() = runBlocking {
        observationScope.cancel()
        withContext(Dispatchers.Main) { models.clear() }
        val folder = File(fixture.context.filesDir, "${PhotoFiles.DIRECTORY}/$owner")
        folder.listFiles()?.forEach { it.delete() }
        folder.delete()
        fixture.close()
    }

    @Test fun loadingContentMissingAndTypedLoadFailureRemainDistinct() = runBlocking {
        assertEquals(TransactionDetailUiState.Loading, vm.uiState.value)
        observe()
        assertEquals(TransactionDetailUiState.Content(original), vm.uiState.value)
        repository.row.value = null
        withTimeout(5_000) { vm.uiState.first { it == TransactionDetailUiState.NotFound } }
        val failing = RecordingTransactionRepository().apply {
            observation = flow { throw DataAccessException.NetworkUnavailable() }
        }
        val failed = create(failing, "load-error")
        observe(failed)
        assertEquals(TransactionDetailUiState.Error(R.string.error_network_unavailable), failed.uiState.value)
    }

    @Test fun failedEditingAndDeletionPreserveTheRecordAndNeverPublishSuccess() = runBlocking {
        observe()
        repository.onDetails = { _, _, _, _ -> throw DataAccessException.AccessDenied() }
        withContext(Dispatchers.Main) {
            assertTrue(vm.prepareEdit())
            vm.updateEditAmount("50.25")
            vm.updateTransaction()
        }
        withTimeout(5_000) { while (vm.actionResult == null) delay(10) }
        assertTrue(vm.actionResult is TransactionActionResult.Failure)
        assertEquals(original, repository.row.value)
        assertEquals("50.25", vm.editAmount)
        repository.onDelete = { throw DataAccessException.NetworkUnavailable() }
        withContext(Dispatchers.Main) { vm.consumeActionResult(); vm.deleteTransaction() }
        withTimeout(5_000) { while (vm.actionResult == null) delay(10) }
        assertTrue(vm.actionResult is TransactionActionResult.Failure)
        assertEquals(original, repository.row.value)
    }

    @Test fun deletingTheRowCanPublishDeletionAfterTheObservationBecomesNotFound() = runBlocking {
        observe()
        withContext(Dispatchers.Main) { vm.deleteTransaction() }
        withTimeout(5_000) { while (vm.actionResult != TransactionActionResult.Deleted) delay(10) }
        assertNull(repository.row.value)
        withTimeout(5_000) { vm.uiState.first { it == TransactionDetailUiState.NotFound } }
        assertEquals(TransactionActionResult.Deleted, vm.actionResult)
    }

    @Test fun anOldAccountFailureDoesNotReachTheNewAccount() = runBlocking {
        observe()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        repository.onDetails = { _, _, _, _ ->
            entered.complete(Unit)
            release.await()
            finished.complete(Unit)
            throw DataAccessException.AccessDenied()
        }
        try {
            withContext(Dispatchers.Main) { vm.prepareEdit(); vm.updateTransaction() }
            withTimeout(5_000) { entered.await() }
            fixture.activate("B", "EUR")
            release.complete(Unit)
            withTimeout(5_000) { finished.await() }
            withContext(Dispatchers.Main) { yield() }
            assertNull(vm.actionResult)
            assertEquals(original, repository.row.value)
        } finally { release.complete(Unit) }
    }

    @Test fun serializedPhotoUpdatesFinishWithTheLatestSelection() = runBlocking {
        observe()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val paths = mutableListOf<String>()
        repository.onPhoto = { _, path ->
            paths += checkNotNull(path)
            if (paths.size == 1) { entered.complete(Unit); release.await() }
            val previous = repository.row.value?.photoUri
            repository.row.value = checkNotNull(repository.row.value).copy(photoUri = path)
            previous
        }
        ReceiptImage(fixture.context).use { first ->
            ReceiptImage(fixture.context).use { second ->
                try {
                    withContext(Dispatchers.Main) { vm.onPhotoSelected(first.uri) }
                    withTimeout(5_000) { entered.await() }
                    withContext(Dispatchers.Main) { vm.onPhotoSelected(second.uri) }
                    release.complete(Unit)
                    withTimeout(5_000) { while (paths.size != 2) delay(10) }
                    withTimeout(5_000) { vm.uiState.first { it.transaction?.photoUri == paths.last() } }
                    assertNotEquals(paths.first(), paths.last())
                    assertEquals(paths.last(), repository.row.value?.photoUri)
                    // The queued upload still owns the superseded file until its operation retires.
                    assertTrue(File(paths.first()).isFile)
                    assertTrue(fixture.database.photoOperationDao().forAccount(owner).any { it.path == paths.first() })
                    assertTrue(File(paths.last()).isFile)
                    assertEquals(original.note, repository.row.value?.note)
                } finally { release.complete(Unit) }
            }
        }
    }

    @Test fun plainDecimalEditingRoundTripsLargeFractionalAndWholeAmountsWithoutChangingMoney() = runBlocking {
        observe()
        for ((amount, expected) in listOf(10000000.0 to "10000000", 10000000.01 to "10000000.01", 100.0 to "100", 25.50 to "25.5", 0.01 to "0.01")) {
            repository.row.value = original.copy(amount = amount)
            withTimeout(5_000) { vm.uiState.first { it.transaction?.amount == amount } }
            withContext(Dispatchers.Main) {
                vm.consumeActionResult()
                assertTrue(vm.prepareEdit())
                assertEquals(expected, vm.editAmount)
                vm.updateTransaction()
            }
            withTimeout(5_000) { while (vm.actionResult == null) delay(10) }
            assertEquals(TransactionActionResult.Updated, vm.actionResult)
            assertEquals(amount, checkNotNull(repository.row.value).amount, 0.0)
        }
    }

    @Test fun invalidAmountsAndMissingCategoryNeverReachTheDetailsMutation() = runBlocking {
        observe()
        repository.onDetails = { _, _, _, _ -> error("Invalid edit must not reach data") }
        withContext(Dispatchers.Main) {
            listOf("", "0", "-1", "NaN", "Infinity", "1.001").forEach {
                vm.updateEditAmount(it)
                vm.updateTransaction()
                assertEquals(TransactionActionResult.Failure(R.string.invalid_amount), vm.actionResult)
            }
            vm.updateEditAmount("10")
            vm.updateTransaction()
            assertEquals(TransactionActionResult.Failure(R.string.error_select_category), vm.actionResult)
        }
        assertEquals(original, repository.row.value)
    }

    private suspend fun savedPhoto(): String = ReceiptImage(fixture.context).use {
        checkNotNull(PhotoLocalStore(fixture.context, fixture.database, Dispatchers.IO, fixture.clock).save(it.uri, null, owner))
    }

    @Test fun failedPhotoDeletionRetainsTheFileAndRetryPublishesOnlySuccessfulDeletion() = runBlocking {
        val path = savedPhoto()
        repository.row.value = original.copy(photoUri = path)
        observe()
        repository.onPhoto = { _, _ -> throw DataAccessException.AccessDenied() }
        withContext(Dispatchers.Main) { vm.deletePhoto() }
        withTimeout(5_000) { while (vm.photoErrorResId == null) delay(10) }
        assertEquals(R.string.photo_delete_failed, vm.photoErrorResId)
        assertNull(vm.actionResult)
        assertTrue(File(path).isFile)
        assertEquals(path, repository.row.value?.photoUri)
        repository.onPhoto = { target, updated -> repository.row.value = target.copy(photoUri = updated); target.photoUri }
        withContext(Dispatchers.Main) { vm.deletePhoto() }
        withTimeout(5_000) { while (vm.actionResult != TransactionActionResult.PhotoDeleted) delay(10) }
        assertNull(repository.row.value?.photoUri)
        assertFalse(File(path).exists())
    }

    @Test fun failedPhotoReplacementDeletesOnlyItsNewPreparedFileAndSuppressesOldAccountErrors() = runBlocking {
        val old = savedPhoto()
        repository.row.value = original.copy(photoUri = old)
        observe()
        var prepared: String? = null
        repository.onPhoto = { _, path -> prepared = path; throw DataAccessException.NetworkUnavailable() }
        ReceiptImage(fixture.context).use { image ->
            withContext(Dispatchers.Main) { vm.onPhotoSelected(image.uri) }
            withTimeout(5_000) { while (prepared == null || File(checkNotNull(prepared)).exists()) delay(10) }
            assertEquals(R.string.photo_save_failed, vm.photoErrorResId)
            assertTrue(File(old).isFile)
            assertEquals(old, repository.row.value?.photoUri)
        }
        prepared = null
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        repository.onPhoto = { _, path -> prepared = path; entered.complete(Unit); release.await(); throw DataAccessException.NetworkUnavailable() }
        ReceiptImage(fixture.context).use { image ->
            try {
                withContext(Dispatchers.Main) { vm.onPhotoSelected(image.uri) }
                withTimeout(5_000) { entered.await() }
                fixture.activate("B", "EUR")
                release.complete(Unit)
                withTimeout(5_000) { while (File(checkNotNull(prepared)).exists()) delay(10) }
                assertTrue(File(old).isFile)
                assertEquals(old, repository.row.value?.photoUri)
                assertNull(vm.photoErrorResId)
                assertNull(vm.actionResult)
            } finally { release.complete(Unit) }
        }
    }

    @Test fun replacingPhotoNeverDeletesAPreviousFileStillReferencedByAnotherRecord() = runBlocking {
        val old = savedPhoto()
        repository.row.value = original.copy(photoUri = old)
        fixture.database.transactionDao().insertTransaction(original.copy(id = 0, firestoreId = "other-reference", photoUri = old).toEntity())
        observe()
        ReceiptImage(fixture.context).use { image ->
            withContext(Dispatchers.Main) { vm.onPhotoSelected(image.uri) }
            withTimeout(5_000) { vm.uiState.first { it.transaction?.photoUri != old } }
            assertTrue(File(old).isFile)
            assertTrue(File(checkNotNull(repository.row.value?.photoUri)).isFile)
        }
    }

    @Test fun previousUnreferencedPhotoIsDeletedOnlyAfterTheReplacementCommits() = runBlocking {
        val old = savedPhoto()
        repository.row.value = original.copy(photoUri = old)
        observe()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        repository.onPhoto = { target, path ->
            entered.complete(Unit); release.await()
            repository.row.value = target.copy(photoUri = path)
            target.photoUri
        }
        ReceiptImage(fixture.context).use { image ->
            try {
                withContext(Dispatchers.Main) { vm.onPhotoSelected(image.uri) }
                withTimeout(5_000) { entered.await() }
                assertTrue(File(old).isFile)
                assertEquals(old, repository.row.value?.photoUri)
                release.complete(Unit)
                withTimeout(5_000) { while (File(old).exists()) delay(10) }
                assertFalse(File(old).exists())
                assertTrue(File(checkNotNull(repository.row.value?.photoUri)).isFile)
            } finally { release.complete(Unit) }
        }
    }

    @Test fun deleteQueuedDuringPhotoCommitRemovesTheLatestCommittedPhotoWithoutReopeningIt() = runBlocking {
        observe()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var saved: String? = null
        repository.onPhoto = { _, path ->
            if (path != null) { saved = path; entered.complete(Unit); release.await() }
            val previous = repository.row.value?.photoUri
            repository.row.value = checkNotNull(repository.row.value).copy(photoUri = path)
            previous
        }
        ReceiptImage(fixture.context).use { image ->
            try {
                withContext(Dispatchers.Main) { vm.onPhotoSelected(image.uri) }
                withTimeout(5_000) { entered.await() }
                withContext(Dispatchers.Main) { vm.deletePhoto() }
                release.complete(Unit)
                withTimeout(5_000) { while (vm.actionResult != TransactionActionResult.PhotoDeleted) delay(10) }
                assertNull(repository.row.value?.photoUri)
                assertTrue(File(checkNotNull(saved)).isFile)
                assertTrue(fixture.database.photoOperationDao().forAccount(owner).any { it.path == saved })
            } finally { release.complete(Unit) }
        }
    }
}
