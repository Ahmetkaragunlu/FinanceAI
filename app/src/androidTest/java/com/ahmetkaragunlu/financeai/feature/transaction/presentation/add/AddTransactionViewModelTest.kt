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
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import com.ahmetkaragunlu.financeai.feature.transaction.testing.RecordingTransactionRepository
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddTransactionViewModelTest {
    private lateinit var fixture: AccountDatabaseFixture
    private lateinit var vm: AddTransactionViewModel
    private val models = ViewModelStore()
    private val transactions = RecordingTransactionRepository()
    private val schedules = Plans()
    private val owner = "add-vm-${UUID.randomUUID()}"
    private var onResolve: suspend (Coordinates) -> LocationData? =
        { error("No address request expected") }

    private class Plans : ScheduledTransactionRepository {
        val inserted = mutableListOf<ScheduledTransaction>()
        override suspend fun insertScheduledTransaction(transaction: ScheduledTransaction): Long {
            inserted += transaction; return 1
        }

        override suspend fun deleteScheduledTransaction(transaction: ScheduledTransaction): Unit =
            unused()

        override fun observeScheduledTransactions(): Flow<List<ScheduledTransaction>> = unused()
        override suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransaction =
            unused()

        override suspend fun getScheduledTransactionById(localId: Long): ScheduledTransaction =
            unused()

        private fun unused(): Nothing = error("Unexpected add plan operation")
    }

    @Before
    fun setup() = runBlocking {
        fixture = AccountDatabaseFixture()
        fixture.activate(owner)
        val addresses = object : AddressResolver {
            override suspend fun resolve(coordinates: Coordinates): LocationData? =
                onResolve(coordinates)

            override suspend fun search(query: String): Coordinates = error("No search expected")
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

    @After
    fun close() = runBlocking {
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

    @Test
    fun invalidAmountAndCategoryNeverReachEitherRepository() = runBlocking {
        withContext(Dispatchers.Main) { vm.saveTransaction() }
        assertEquals(
            TransactionActionResult.Failure(R.string.error_invalid_amount),
            vm.actionResult
        )
        withContext(Dispatchers.Main) { vm.updateInputAmount("10"); vm.saveTransaction() }
        assertEquals(
            TransactionActionResult.Failure(R.string.error_select_category),
            vm.actionResult
        )
        assertTrue(transactions.insertions.isEmpty())
        assertTrue(schedules.inserted.isEmpty())
    }

    @Test
    fun repeatedSubmissionRetainsTheOriginalSnapshotAndOnlyOneInsertion() = runBlocking {
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
        } finally {
            release.complete(Unit)
        }
    }

    @Test
    fun reminderDraftRoutesToThePlanRepositoryWithoutCreatingImmediateMoney() = runBlocking {
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

    @Test
    fun failedPhotoPreparationKeepsTheFormAndDoesNotCommitMoney() = runBlocking {
        fill()
        withContext(Dispatchers.Main) {
            vm.onPhotoSelected(
                Uri.fromFile(
                    File(
                        fixture.context.cacheDir,
                        "missing-${UUID.randomUUID()}.jpg"
                    )
                )
            )
            vm.saveTransaction()
        }
        waitForResult()
        assertEquals(TransactionActionResult.Failure(R.string.photo_save_failed), vm.actionResult)
        assertEquals("25.50", vm.inputAmount)
        assertTrue(transactions.insertions.isEmpty())
    }

    @Test
    fun failedInsertionRemovesPreparedPhotoButPreservesTheDraft() = runBlocking {
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

    @Test
    fun aLateOldAccountCommitDoesNotClearTheNewDraftOrPublishSuccess() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        transactions.onInsert =
            { entered.complete(Unit); release.await(); finished.complete(Unit); 1 }
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
        } finally {
            release.complete(Unit)
        }
    }

    @Test
    fun reminderDatesUseAccountZoneWhileNormalDraftDatesRetainDeviceZone() = runBlocking {
        val zone = ZoneId.of("Asia/Tokyo")
        fixture.activate(owner, "USD", zone.id)
        val now = Clock.fixed(Instant.parse("2026-10-08T23:30:00Z"), ZoneOffset.UTC)
        val addresses = object : AddressResolver {
            override suspend fun resolve(coordinates: Coordinates): LocationData? = null
            override suspend fun search(query: String): Coordinates? = null
        }
        withContext(Dispatchers.Main) {
            vm = AddTransactionViewModel(
                PhotoLocalStore(fixture.context, fixture.database, Dispatchers.IO, now),
                fixture.session,
                transactions,
                schedules,
                PhotoWorkScheduler(fixture.workManager, fixture.session, fixture.database),
                addresses,
                now,
                fixture.context
            ).also { models.put("add", it) }
            assertEquals(ZoneId.systemDefault(), vm.dateZone())
            assertTrue(vm.isDateValid(now.millis()))
            assertFalse(vm.isDateValid(now.millis() + 1))
            vm.toggleReminder(true)
            assertEquals(zone, vm.dateZone())
            assertEquals(
                LocalDate.of(2026, 10, 10).atStartOfDay(zone).toInstant().toEpochMilli(),
                vm.selectedDate
            )
            val today = LocalDate.of(2026, 10, 9).atStartOfDay(zone).toInstant().toEpochMilli()
            assertTrue(vm.isDateValid(today))
            assertFalse(vm.isDateValid(today - 1))
            val picker =
                LocalDate.of(2026, 10, 9).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            vm.selectPickerDate(picker)
            assertEquals(today, vm.selectedDate)
            assertEquals(picker, vm.pickerDate())
            assertTrue(vm.isPickerDateValid(picker))
            assertFalse(vm.isPickerDateValid(picker - 86_400_000))
            vm.toggleReminder(false)
            assertEquals(now.millis(), vm.selectedDate)
            assertEquals(ZoneId.systemDefault(), vm.dateZone())
        }
    }

    @Test
    fun sameTypeKeepsDraftAndChangedTypeClearsOnlyTheExistingDraftFields() = runBlocking {
        onResolve = { LocationData(it.latitude, it.longitude, "resolved", "address") }
        fill()
        withContext(Dispatchers.Main) { vm.toggleReminder(true); vm.onLocationSelected(1.0, 2.0) }
        withTimeout(5_000) { while (vm.selectedLocation == null) delay(10) }
        withContext(Dispatchers.Main) {
            vm.updateTransactionType(TransactionType.EXPENSE)
            assertEquals("25.50", vm.inputAmount)
            assertTrue(vm.isReminderEnabled)
            assertEquals(CategoryType.FOOD, vm.selectedCategory)
            vm.updateTransactionType(TransactionType.INCOME)
            assertNull(vm.selectedCategory)
            assertEquals("", vm.inputAmount)
            assertEquals("", vm.inputNote)
            assertNull(vm.selectedLocation)
            assertNull(vm.selectedPhotoUri)
            assertFalse(vm.isReminderEnabled)
            assertEquals(fixture.clock.millis(), vm.selectedDate)
            assertTrue(vm.availableCategories.all { it.type == TransactionType.INCOME })
        }
    }

    @Test
    fun lateLocationCannotOverwriteANewerSelectionOrReturnAfterClearOrAccountSwitch() =
        runBlocking {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            onResolve = {
                if (it.latitude == 1.0) withContext(NonCancellable) { entered.complete(Unit); release.await() }
                LocationData(it.latitude, it.longitude, "${it.latitude}", "address")
            }
            try {
                withContext(Dispatchers.Main) { vm.onLocationSelected(1.0, 1.0) }
                withTimeout(5_000) { entered.await() }
                withContext(Dispatchers.Main) { vm.onLocationSelected(2.0, 2.0) }
                withTimeout(5_000) { while (vm.selectedLocation?.latitude != 2.0) delay(10) }
                release.complete(Unit)
                withContext(Dispatchers.Main) { yield() }
                assertEquals(2.0, checkNotNull(vm.selectedLocation).latitude, 0.0)
                val gate = CompletableDeferred<Unit>()
                val started = CompletableDeferred<Unit>()
                onResolve =
                    { withContext(NonCancellable) { started.complete(Unit); gate.await() }; null }
                try {
                    withContext(Dispatchers.Main) { vm.onLocationSelected(3.0, 3.0) }
                    withTimeout(5_000) { started.await() }
                    withContext(Dispatchers.Main) { vm.clearLocation() }
                    gate.complete(Unit)
                    withContext(Dispatchers.Main) { yield() }
                    assertNull(vm.selectedLocation)
                    assertNull(vm.feedbackMessageRes)
                } finally {
                    gate.complete(Unit)
                }
                val accountGate = CompletableDeferred<Unit>()
                val accountStarted = CompletableDeferred<Unit>()
                onResolve =
                    { withContext(NonCancellable) { accountStarted.complete(Unit); accountGate.await() }; null }
                try {
                    withContext(Dispatchers.Main) { vm.onLocationSelected(4.0, 4.0) }
                    withTimeout(5_000) { accountStarted.await() }
                    fixture.activate("B", "EUR")
                    accountGate.complete(Unit)
                    withContext(Dispatchers.Main) { yield() }
                    assertNull(vm.selectedLocation)
                    assertNull(vm.feedbackMessageRes)
                } finally {
                    accountGate.complete(Unit)
                }
            } finally {
                release.complete(Unit)
            }
        }

    @Test
    fun addressFallbackIsSavedUnchangedAndResolverFailureDoesNotInventAnAddress() = runBlocking {
        onResolve = { null }
        fill()
        withContext(Dispatchers.Main) { vm.onLocationSelected(41.12, 29.34) }
        withTimeout(5_000) { while (vm.selectedLocation == null) delay(10) }
        val expected = fixture.context.getString(R.string.location_coordinates, 41.12, 29.34)
        assertEquals(expected, vm.selectedLocation?.addressFull)
        assertEquals(R.string.address_not_found, vm.feedbackMessageRes)
        onResolve = { throw IllegalStateException("private") }
        withContext(Dispatchers.Main) { vm.consumeFeedback(); vm.onLocationSelected(1.0, 2.0) }
        withTimeout(5_000) { while (vm.feedbackMessageRes == null) delay(10) }
        assertEquals(expected, vm.selectedLocation?.addressFull)
        withContext(Dispatchers.Main) { vm.saveTransaction() }
        waitForResult()
        val saved = transactions.insertions.single()
        assertEquals(expected, saved.locationFull)
        assertEquals(41.12, checkNotNull(saved.latitude), 0.0)
        assertEquals(29.34, checkNotNull(saved.longitude), 0.0)
        assertEquals(fixture.context.getString(R.string.location_label), saved.locationShort)
    }
}
