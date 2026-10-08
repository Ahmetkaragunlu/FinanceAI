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
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import com.ahmetkaragunlu.financeai.feature.transaction.testing.RecordingTransactionRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Assert.*
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
                    assertTrue(File(paths.last()).isFile)
                    assertEquals(original.note, repository.row.value?.note)
                } finally { release.complete(Unit) }
            }
        }
    }
}
