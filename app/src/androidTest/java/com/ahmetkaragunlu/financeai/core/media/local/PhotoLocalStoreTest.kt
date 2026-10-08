package com.ahmetkaragunlu.financeai.core.media.local

import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import java.io.File
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class PhotoLocalStoreTest {
    private lateinit var context: Context
    private lateinit var database: FinanceDatabase
    private lateinit var store: PhotoLocalStore
    private lateinit var owner: String
    private lateinit var folder: File
    @Before fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        owner = "photo-test-${UUID.randomUUID()}"
        folder = File(context.filesDir, "${PhotoFiles.DIRECTORY}/$owner").apply { mkdirs() }
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        store = PhotoLocalStore(context, database, Dispatchers.IO, Clock.systemUTC())
    }
    @After fun close() { database.close(); folder.deleteRecursively() }
    private fun image(width: Int, height: Int): File {
        val file = File(folder, "TEMP_input.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try { file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) } }
        finally { bitmap.recycle() }
        return file
    }
    @Test fun cameraAndGalleryUseBoundedOrientationAwarePreparation() = runBlocking {
        val input = image(3000, 1000)
        ExifInterface(input.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val result = store.save(Uri.fromFile(input), input.absolutePath, owner)!!
        val bitmap = BitmapFactory.decodeFile(result)
        try { assertEquals(640, bitmap.width); assertEquals(1920, bitmap.height) } finally { bitmap.recycle() }
        assertFalse(input.exists())
        assertFalse(folder.listFiles()!!.any { it.name.endsWith(".part") })
    }
    @Test fun corruptInputDoesNotCreatePermanentOrPartialFiles() = runBlocking {
        val input = File(folder, "TEMP_bad.jpg").apply { writeText("not an image") }
        assertNull(store.save(Uri.fromFile(input), input.absolutePath, owner))
        assertTrue(input.exists())
        assertEquals(listOf(input.name), folder.listFiles()!!.map { it.name })
    }
    @Test fun anotherOwnerFileIsNeverConsumedAsThisAccountsCameraDraft(): Unit = runBlocking {
        val input = image(64, 32)
        assertNull(store.save(Uri.fromFile(input), input.absolutePath, "other-$owner"))
        assertTrue(input.exists())
        File(context.filesDir, "${PhotoFiles.DIRECTORY}/other-$owner").deleteRecursively()
    }
    @Test fun pendingUploadProtectsItsFileFromDeletionUntilAcknowledged() = runBlocking {
        val input = image(64, 32)
        database.photoOperationDao().insert(PhotoOperation(owner, "transactions", "record", input.absolutePath, "v1"))
        assertFalse(store.delete(input.absolutePath))
        assertTrue(input.exists())
        database.photoOperationDao().acknowledge(owner, "transactions", "record", input.absolutePath)
        assertTrue(store.delete(input.absolutePath))
    }
    @Test fun cleanupDoesNotDeleteFilesOutsideTheManagedDirectory() = runBlocking {
        val outside = File(context.cacheDir, "photo-outside-${UUID.randomUUID()}").apply { writeText("keep") }
        try { assertFalse(store.delete(outside.absolutePath)); assertTrue(outside.exists()) } finally { outside.delete() }
    }
}
