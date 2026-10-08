package com.ahmetkaragunlu.financeai.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import com.ahmetkaragunlu.financeai.core.coroutines.di.IoDispatcher
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.local.PhotoFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.time.Clock
import java.time.Duration
import androidx.room.withTransaction
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class PhotoLocalStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FinanceDatabase,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val clock: Clock
) {
    /** ImageDecoder performs orientation-aware bounded decoding on every supported device (minSdk 30). */
    suspend fun save(uri: Uri, cameraPath: String?, ownerId: String): String? = withContext(io) {
        require(ownerId.isNotBlank() && '/' !in ownerId && ownerId != "." && ownerId != "..")
        val folder = File(context.filesDir, "${PhotoFiles.DIRECTORY}/$ownerId").canonicalFile
        check(folder.isDirectory || folder.mkdirs())
        val target = File(folder, "IMG_${UUID.randomUUID()}.jpg")
        var part: File? = null
        var bitmap: Bitmap? = null
        try {
            val camera = cameraPath?.let { File(it).canonicalFile.also { file ->
                require(file.parentFile == folder && file.name.startsWith("TEMP_") && file.isFile)
            } }
            val source = if (camera != null) ImageDecoder.createSource(camera)
                else ImageDecoder.createSource(context.contentResolver, uri)
            bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                require(info.mimeType.startsWith("image/"))
                val (width, height) = PhotoDimensions.target(info.size.width, info.size.height)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSize(width, height)
                decoder.setOnPartialImageListener { false }
            }
            currentCoroutineContext().ensureActive()
            part = File.createTempFile("PREP_", ".part", folder)
            part.outputStream().use { output ->
                check(checkNotNull(bitmap).compress(Bitmap.CompressFormat.JPEG, PhotoDimensions.JPEG_QUALITY, output))
                output.fd.sync()
            }
            currentCoroutineContext().ensureActive()
            check(part.renameTo(target))
            camera?.delete()
            target.absolutePath
        } catch (e: CancellationException) {
            target.delete()
            throw e
        } catch (e: Exception) {
            target.delete()
            Log.w("PhotoLocalStore", "Image preparation failed (${e.javaClass.simpleName})")
            null
        } finally { bitmap?.recycle(); part?.delete() }
    }

    suspend fun delete(path: String?) = withContext(io) {
        val file = ownedFile(path) ?: return@withContext false
        database.withTransaction {
            // Android exposes /data/user/0 and /data/data aliases for the same application file.
            val root = File(context.filesDir, PhotoFiles.DIRECTORY)
            val appPath = File(root, file.relativeTo(root.canonicalFile).path).absolutePath
            val aliases = setOf(checkNotNull(path), appPath, file.absolutePath)
            if (aliases.any { database.photoOperationDao().isReferenced(it) }) false else file.delete()
        }
    }
    /** Sweep only known permanent/cache shapes, never unknown/active camera drafts. */
    suspend fun cleanUnreferenced(ownerId: String) = withContext(io) {
        require(ownerId.isNotBlank() && '/' !in ownerId && ownerId != "." && ownerId != "..")
        val folder = File(context.filesDir, "${PhotoFiles.DIRECTORY}/$ownerId")
        val cutoff = clock.millis() - Duration.ofDays(1).toMillis()
        folder.listFiles()?.filter { it.isFile && it.lastModified() < cutoff && (it.name.startsWith("IMG_") || it.name.startsWith("SYNC_")) }
            ?.forEach { delete(it.absolutePath) }
    }
    private fun ownedFile(path: String?): File? {
        if (path.isNullOrBlank() || path.startsWith("http")) return null
        val root = File(context.filesDir, PhotoFiles.DIRECTORY).canonicalFile
        val file = File(path).canonicalFile
        return file.takeIf { it.parentFile?.parentFile == root && it.isFile }
    }
}
