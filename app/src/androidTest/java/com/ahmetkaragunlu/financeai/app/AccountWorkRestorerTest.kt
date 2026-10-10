package com.ahmetkaragunlu.financeai.app

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.local.PhotoFiles
import com.ahmetkaragunlu.financeai.core.media.local.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class AccountWorkRestorerTest {
    private class Fixture : AutoCloseable {
        val local = AccountDatabaseFixture()
        val owner = "restore-${UUID.randomUUID()}"
        val folder = File(local.context.filesDir, "${PhotoFiles.DIRECTORY}/$owner")
        lateinit var image: File
        val photos = mock(PhotoWorkScheduler::class.java)
        val files = mock(PhotoLocalStore::class.java)
        val tokens = mock(FCMTokenManager::class.java)
        val reminders = spy(ReminderScheduler(local.workManager, local.clock))
        val events = mutableListOf<String>()
        var switchAfterUpload = false
        val restorer = AccountWorkRestorer(local.database, local.session, reminders, photos, files, tokens)

        suspend fun prepare() {
            local.activate(owner)
            folder.mkdirs()
            image = withContext(Dispatchers.IO) {
                File.createTempFile(PhotoFiles.PERMANENT_PREFIX, ".jpg", folder)
            }
            val row = Transaction(ownerId = owner, currencyCode = "USD", firestoreId = "receipt", amount = 10.0,
                date = 100, transaction = TransactionType.EXPENSE, category = CategoryType.FOOD, note = "", photoUri = image.path)
            local.database.transactionDao().insertTransaction(row.toEntity())
            local.database.transactionDao().insertTransaction(row.copy(firestoreId = "remote-image", photoUri = "https://synthetic.invalid/image").toEntity())
            local.database.transactionDao().insertTransaction(row.copy(ownerId = "foreign", firestoreId = "foreign").toEntity())
            local.database.scheduledTransactionDao().insertScheduledTransaction(ScheduledTransaction(ownerId = owner,
                currencyCode = "USD", firestoreId = "plan", amount = 5.0, type = TransactionType.EXPENSE,
                category = CategoryType.FOOD, note = null, scheduledDate = 200, photoUri = File(folder, "missing.jpg").path).toEntity())
            val operation = PhotoOperation(owner, "scheduled", "operation", image.path, "synthetic-version")
            local.database.photoOperationDao().insert(operation)
            local.database.photoOperationDao().insert(operation.copy(remoteId = "failed", failure = "permission_denied"))
            doAnswer {
                events += "receipt-photo"
                if (switchAfterUpload) local.session.activate("other-account", "EUR")
                Unit
            }.`when`(photos).upload(owner, PhotoRecordType.TRANSACTION, "receipt", image.path)
            doAnswer { events += "plan-reminder"; null }.`when`(reminders).wake(owner, "plan", local.clock.millis())
            doAnswer { events += "operation-photo"; }.`when`(photos).upload(owner, "scheduled", "operation", image.path)
            doAnswer { events += "tokens"; null }.`when`(tokens).restore(owner)
            doAnswer { events += "file-cleanup"; }.`when`(files).cleanUnreferenced(owner)
        }

        override fun close() {
            folder.listFiles()?.forEach { it.delete() }
            folder.delete()
            local.close()
        }
    }

    @Test fun currentAccountRestoresOnlyLocalFilesEligibleOperationsAndItsOwnReminderAndTokenWork() = runBlocking {
        val f = Fixture()
        f.use { f ->
            f.prepare()
            f.restorer.restore(f.local.session.requireAccount())
            assertEquals(listOf("receipt-photo", "plan-reminder", "operation-photo", "tokens", "file-cleanup"), f.events)
            verify(f.photos).upload(f.owner, PhotoRecordType.TRANSACTION, "receipt", f.image.path)
            verify(f.photos).upload(f.owner, "scheduled", "operation", f.image.path)
            verify(f.reminders).wake(f.owner, "plan", f.local.clock.millis())
            verify(f.tokens).restore(f.owner)
            verify(f.files).cleanUnreferenced(f.owner)
            verifyNoMoreInteractions(f.photos, f.reminders, f.tokens, f.files)
        }
    }

    @Test fun switchingAccountsDuringRestorationStopsEveryRemainingOldAccountDispatch() = runBlocking {
        val f = Fixture()
        f.use { f ->
            f.prepare()
            val original = f.local.session.requireAccount()
            f.switchAfterUpload = true
            f.restorer.restore(original)
            assertEquals(listOf("receipt-photo"), f.events)
            f.events.clear()
            f.restorer.restore(original)
            assertEquals(emptyList<String>(), f.events)
            verify(f.photos).upload(f.owner, PhotoRecordType.TRANSACTION, "receipt", f.image.path)
            verifyNoMoreInteractions(f.photos, f.reminders, f.tokens, f.files)
        }
    }
}
