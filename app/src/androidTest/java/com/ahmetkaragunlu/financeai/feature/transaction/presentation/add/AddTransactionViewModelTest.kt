package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.media.local.PhotoFiles
import com.ahmetkaragunlu.financeai.core.media.local.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.media.testing.ReceiptImage
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import com.ahmetkaragunlu.financeai.feature.location.domain.AddressResolver
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import com.ahmetkaragunlu.financeai.feature.transaction.testing.RecordingTransactionRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AddTransactionViewModelTest {
    private lateinit var fixture: AccountDatabaseFixture
    private lateinit var vm: AddTransactionViewModel
    private val models = ViewModelStore()
    private val transactions = RecordingTransactionRepository()
    private val schedules = Plans()
    private val owner = "add-vm-${UUID.randomUUID()}"

    private class Plans : ScheduledTransactionRepository {
        val inserted = mutableListOf<ScheduledTransaction>()
        override suspend fun insertScheduledTransaction(transaction: ScheduledTransaction): Long { inserted += transaction; return 1 }
        override suspend fun updateScheduledTransaction(transaction: ScheduledTransaction): Unit = unused()
        override suspend fun deleteScheduledTransaction(transaction: ScheduledTransaction): Unit = unused()
        override fun observeScheduledTransactions(): Flow<List<ScheduledTransaction>> = unused()
        override suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransaction? = unused()
        override suspend fun getScheduledTransactionById(localId: Long): ScheduledTransaction? = unused()
        private fun unused(): Nothing = error("Unexpected add plan operation")
    }

    @Before fun setup() = runBlocking {
        fixture = AccountDatabaseFixture()
        fixture.activate(owner)
        val addresses = object : AddressResolver {
            override suspend fun resolve(coordinates: Coordinates): LocationData? = error("No address request expected")
            override suspend fun search(query: String): Coordinates? = error("No search expected")
        }
        withContext(Dispatchers.Main) {
            vm = AddTransactionViewModel(
                PhotoLocalStore(fixture.context, fixture.database, Dispatchers.IO, fixture.clock),
                fixture.session, transactions, schedules,
                PhotoWorkScheduler(fixture.workManager, fixture.session, fixture.database),
                addresses, fixture.clock, fixture.context
            )
            models.put("add", vm)
        }
    }

    @After fun close() = runBlocking {
        withContext(Dispatchers.Main) { models.clear() }
        val folder = File(fixture.context.filesDir, "${PhotoFiles.DIRECTORY}/$owner")
        folder.listFiles()?.forEach { it.delete() }
        folder.delete()
        fixture.close()
    }

    private suspend fun fill() = withContext(Dispatchers.Main) {
        vm.updateInputAmount("25.50")
        vm.updateInputNote("receipt")
        vm.updateCategory(CategoryType.FOOD)
    }
    private suspend fun waitForResult() = withTimeout(5_000) {
        while (withContext(Dispatchers.Main) { vm.actionResult == null }) delay(10)
    }

    @Test fun invalidAmountAndCategoryNeverReachEitherRepository() = runBlocking {
        withContext(Dispatchers.Main) { vm.saveTransaction() }
        assertEquals(TransactionActionResult.Failure(R.string.error_invalid_amount), vm.actionResult)
        withContext(Dispatchers.Main) { vm.updateInputAmount("10"); vm.saveTransaction() }
        assertEquals(TransactionActionResult.Failure(R.string.error_select_category), vm.actionResult)
        assertTrue(transactions.insertions.isEmpty())
        assertTrue(schedules.inserted.isEmpty())
    }

    @Test fun repeatedSubmissionRetainsTheOriginalSnapshotAndOnlyOneInsertion() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        transactions.onInsert = { entered.complete(Unit); release.await(); 1 }
        fill()
        try {
            withContext(Dispatchers.Main) { vm.saveTransaction() }
            withTimeout(5_000) { entered.await() }
            withContext(Dispatchers.Main) { vm.updateInputAmount("99"); vm.saveTransaction() }
            assertEquals(1, transactions.insertions.size)
            assertEquals(25.50, transactions.insertions.single().amount, 0.0)
            release.complete(Unit)
            waitForResult()
            assertEquals(TransactionActionResult.Saved, vm.actionResult)
            assertEquals("", vm.inputAmount)
        } finally { release.complete(Unit) }
    }

    @Test fun reminderDraftRoutesToThePlanRepositoryWithoutCreatingImmediateMoney() = runBlocking {
        fill()
        withContext(Dispatchers.Main) { vm.toggleReminder(true); vm.saveTransaction() }
        waitForResult()
        assertTrue(transactions.insertions.isEmpty())
        val plan = schedules.inserted.single()
        assertEquals(owner, plan.ownerId)
        assertEquals("USD", plan.currencyCode)
        assertEquals(25.50, plan.amount, 0.0)
        assertTrue(plan.scheduledDate > fixture.clock.millis())
        assertEquals(TransactionActionResult.Saved, vm.actionResult)
    }

    @Test fun failedPhotoPreparationKeepsTheFormAndDoesNotCommitMoney() = runBlocking {
        fill()
        withContext(Dispatchers.Main) {
            vm.onPhotoSelected(Uri.fromFile(File(fixture.context.cacheDir, "missing-${UUID.randomUUID()}.jpg")))
            vm.saveTransaction()
        }
        waitForResult()
        assertEquals(TransactionActionResult.Failure(R.string.photo_save_failed), vm.actionResult)
        assertEquals("25.50", vm.inputAmount)
        assertTrue(transactions.insertions.isEmpty())
    }

    @Test fun failedInsertionRemovesPreparedPhotoButPreservesTheDraft() = runBlocking {
        fill()
        transactions.onInsert = { throw DataAccessException.NetworkUnavailable() }
        ReceiptImage(fixture.context).use { image ->
            withContext(Dispatchers.Main) { vm.onPhotoSelected(image.uri); vm.saveTransaction() }
            waitForResult()
            assertTrue(vm.actionResult is TransactionActionResult.Failure)
            assertEquals("25.50", vm.inputAmount)
            val saved = checkNotNull(transactions.insertions.single().photoUri)
            assertFalse(File(saved).exists())
        }
    }

    @Test fun aLateOldAccountCommitDoesNotClearTheNewDraftOrPublishSuccess() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        transactions.onInsert = { entered.complete(Unit); release.await(); finished.complete(Unit); 1 }
        fill()
        try {
            withContext(Dispatchers.Main) { vm.saveTransaction() }
            withTimeout(5_000) { entered.await() }
            fixture.activate("B", "EUR")
            withContext(Dispatchers.Main) { vm.updateInputNote("new draft") }
            release.complete(Unit)
            withTimeout(5_000) { finished.await() }
            withContext(Dispatchers.Main) { yield() }
            assertEquals(owner, transactions.insertions.single().ownerId)
            assertEquals("new draft", vm.inputNote)
            assertNull(vm.actionResult)
        } finally { release.complete(Unit) }
    }
}
