package com.ahmetkaragunlu.financeai.core.media.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotoLocalStoreTest {
    private lateinit var context: Context
    private lateinit var database: FinanceDatabase
    private lateinit var store: PhotoLocalStore
    private lateinit var owner: String
    private lateinit var folder: File
    private val clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC)
    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        owner = "photo-test-${UUID.randomUUID()}"
        folder = File(context.filesDir, "${PhotoFiles.DIRECTORY}/$owner").apply { mkdirs() }
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        store = PhotoLocalStore(context, database, Dispatchers.IO, clock)
    }

    @After
    fun close() {
        database.close(); folder.deleteRecursively()
    }

    private fun image(width: Int, height: Int): File {
        val file = File(folder, "TEMP_input.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream()
                .use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    @Test
    fun cameraAndGalleryUseBoundedOrientationAwarePreparation() = runBlocking {
        val input = image(3000, 1000)
        ExifInterface(input.absolutePath).apply {
            setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString()
            )
            saveAttributes()
        }
        val result = store.save(Uri.fromFile(input), input.absolutePath, owner)!!
        val bitmap = BitmapFactory.decodeFile(result)
        try {
            assertEquals(640, bitmap.width); assertEquals(1920, bitmap.height)
        } finally {
            bitmap.recycle()
        }
        assertFalse(input.exists())
        assertFalse(folder.listFiles()!!.any { it.name.endsWith(".part") })
    }

    @Test
    fun corruptInputDoesNotCreatePermanentOrPartialFiles() = runBlocking {
        val input = File(folder, "TEMP_bad.jpg").apply { writeText("not an image") }
        assertNull(store.save(Uri.fromFile(input), input.absolutePath, owner))
        assertTrue(input.exists())
        assertEquals(listOf(input.name), folder.listFiles()!!.map { it.name })
    }

    @Test
    fun anotherOwnerFileIsNeverConsumedAsThisAccountsCameraDraft(): Unit = runBlocking {
        val input = image(64, 32)
        assertNull(store.save(Uri.fromFile(input), input.absolutePath, "other-$owner"))
        assertTrue(input.exists())
        File(context.filesDir, "${PhotoFiles.DIRECTORY}/other-$owner").deleteRecursively()
    }

    @Test
    fun pendingUploadProtectsItsFileFromDeletionUntilAcknowledged() = runBlocking {
        val input = image(64, 32)
        database.photoOperationDao()
            .insert(PhotoOperation(owner, "transactions", "record", input.absolutePath, "v1"))
        assertFalse(store.delete(input.absolutePath))
        assertTrue(input.exists())
        database.photoOperationDao()
            .acknowledge(owner, "transactions", "record", input.absolutePath)
        assertTrue(store.delete(input.absolutePath))
    }

    @Test
    fun cleanupDoesNotDeleteFilesOutsideTheManagedDirectory() = runBlocking {
        val outside =
            File(context.cacheDir, "photo-outside-${UUID.randomUUID()}").apply { writeText("keep") }
        try {
            assertFalse(store.delete(outside.absolutePath)); assertTrue(outside.exists())
        } finally {
            outside.delete()
        }
    }

    @Test
    fun financialAndPlanReferencesProtectAnOldPhotoUntilItsLastReferenceIsRemoved() = runBlocking {
        val file = File(folder, "IMG_referenced.jpg")
        check(image(64, 32).renameTo(file))
        check(file.setLastModified(clock.millis() - 2 * 86_400_000L))
        val financial = TransactionEntity(
            ownerId = owner, firestoreId = "receipt", currencyCode = "USD",
            amountMinor = 1000, transaction = TransactionType.EXPENSE, category = CategoryType.FOOD,
            date = 100, photoUri = file.absolutePath
        )
        val financialId = database.transactionDao().insertTransaction(financial)
        assertFalse(store.delete(file.absolutePath))
        store.cleanUnreferenced(owner)
        assertTrue(file.isFile)

        val plan = ScheduledTransactionEntity(
            ownerId = owner, firestoreId = "plan", currencyCode = "USD",
            amountMinor = 1000, type = TransactionType.EXPENSE, category = CategoryType.FOOD,
            note = null, scheduledDate = 100, photoUri = file.absolutePath
        )
        val planId = database.scheduledTransactionDao().insertScheduledTransaction(plan)
        database.transactionDao().deleteTransaction(financial.copy(id = financialId.toInt()))
        assertFalse(store.delete(file.absolutePath))
        store.cleanUnreferenced(owner)
        assertTrue(file.isFile)

        database.scheduledTransactionDao().deleteScheduledTransaction(plan.copy(id = planId))
        store.cleanUnreferenced(owner)
        assertFalse(file.exists())
    }

    @Test
    fun cleanupKeepsExactDayBoundaryCameraPreparationAndUnknownShapes(): Unit = runBlocking {
        val cutoff = clock.millis() - 86_400_000L
        val oldPermanent = File(
            folder,
            "IMG_old.png"
        ).apply { writeText("old"); check(setLastModified(cutoff - 1)) }
        val oldCache = File(
            folder,
            "SYNC_old.jpg"
        ).apply { writeText("old"); check(setLastModified(cutoff - 1)) }
        val retained = listOf(
            "IMG_boundary.jpg",
            "TEMP_camera.jpg",
            "PREP_decode.part",
            "unknown.jpg"
        ).map { name ->
            File(
                folder,
                name
            ).apply { writeText("keep"); check(setLastModified(if (name == "IMG_boundary.jpg") cutoff else cutoff - 1)) }
        }
        store.cleanUnreferenced(owner)
        assertFalse(oldPermanent.exists())
        assertFalse(oldCache.exists())
        retained.forEach { assertTrue(it.name, it.isFile) }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { store.cleanUnreferenced("../other") } }
    }

    @Test
    fun canonicalAndroidAliasesCannotBypassAPendingReference(): Unit = runBlocking {
        val file = File(folder, "IMG_alias.jpg").apply { writeText("keep") }
        val alias =
            "/data/data/${context.packageName}/files/${PhotoFiles.DIRECTORY}/$owner/${file.name}"
        assertEquals(file.canonicalFile, File(alias).canonicalFile)
        database.photoOperationDao()
            .insert(PhotoOperation(owner, "transactions", "alias", alias, "v1"))
        assertFalse(store.delete(file.canonicalPath))
        assertTrue(file.isFile)
        database.photoOperationDao().acknowledge(owner, "transactions", "alias", alias)
        assertTrue(store.delete(file.canonicalPath))
    }
}
