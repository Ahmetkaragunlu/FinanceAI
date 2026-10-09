package com.ahmetkaragunlu.financeai.core.media.work

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoWorkSchedulerTest {
    @Test fun typedAndRetainedWireValuesKeepDurableRowsAndAccountCancellationTags() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val scheduler = PhotoWorkScheduler(f.workManager, f.session, f.database)
            val path = File(f.context.cacheDir, "IMG_part1.jpg").path
            scheduler.upload("A", PhotoRecordType.TRANSACTION, "t1", path)
            scheduler.upload("A", "scheduled", "p1", path)
            val operations = f.database.photoOperationDao().forAccount("A")
            assertEquals(setOf("transactions", "scheduled"), operations.map { it.collection }.toSet())
            assertTrue(operations.all { it.version == "IMG_part1" && it.ownerId == "A" })
            for ((collection, record) in listOf("transactions" to "t1", "scheduled" to "p1")) {
                val work = withTimeout(5_000) {
                    f.workManager.getWorkInfosForUniqueWorkFlow("photo_A_${collection}_${record}_IMG_part1")
                        .first { it.isNotEmpty() }.single()
                }
                assertTrue(work.tags.contains("account_A"))
                assertTrue(work.tags.contains(AccountWork.tag("A")))
                assertTrue(work.tags.contains("com.ahmetkaragunlu.financeai.photo.PhotoUploadWorker"))
                assertTrue(f.workManager.getWorkInfosByTagFlow(AccountWork.tag("B")).first().isEmpty())
            }
        }
    }

    @Test fun unknownRetainedCollectionCannotCreateAnOperationOrUploadWork() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val scheduler = PhotoWorkScheduler(f.workManager, f.session, f.database)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { scheduler.upload("A", "scheduled_transactions", "p1", "/test/IMG_part1.jpg") }
            }
            assertTrue(f.database.photoOperationDao().forAccount("A").isEmpty())
            assertTrue(f.workManager.getWorkInfosByTagFlow(AccountWork.tag("A")).first().isEmpty())
        }
    }

    @Test fun existingAccountAndNonUploadablePathGuardsPrecedeRetainedCollectionValidation() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val scheduler = PhotoWorkScheduler(f.workManager, f.session, f.database)
            scheduler.upload("B", "unknown", "p1", "/test/IMG_part1.jpg")
            for (path in listOf(null, "", "https://example.test/receipt", "/test/SYNC_cached.jpg")) {
                scheduler.upload("A", "unknown", "p1", path)
            }
            assertTrue(f.database.photoOperationDao().forAccount("A").isEmpty())
            assertTrue(f.database.photoOperationDao().forAccount("B").isEmpty())
            assertTrue(f.workManager.getWorkInfosByTagFlow(AccountWork.tag("A")).first().isEmpty())
        }
    }
}
