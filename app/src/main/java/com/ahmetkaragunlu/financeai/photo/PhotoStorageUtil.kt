package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.core.graphics.scale

object PhotoStorageUtil {

    const val PHOTO_DIRECTORY = "transaction_photos"
    private const val MAX_IMAGE_SIZE = 1920
    private const val JPEG_QUALITY = 85

    fun savePhotoToInternalStorage(context: Context, photoUri: Uri): String? {
        return try {
            val photoDir = File(context.filesDir, PHOTO_DIRECTORY)
            if (!photoDir.exists()) {
                photoDir.mkdirs()
            }

            val fileName = "IMG_${UUID.randomUUID()}.jpg"
            val photoFile = File(photoDir, fileName)

            val inputStream = context.contentResolver.openInputStream(photoUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val resizedBitmap = resizeBitmap(originalBitmap, MAX_IMAGE_SIZE)

            FileOutputStream(photoFile).use { output ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }

            if (resizedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()

            photoFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createTempPhotoFile(context: Context): Pair<File, Uri>? {
        return try {
            val photoDir = File(context.filesDir, PHOTO_DIRECTORY)
            if (!photoDir.exists()) {
                photoDir.mkdirs()
            }
            val fileName = "TEMP_${System.currentTimeMillis()}.jpg"
            val photoFile = File(photoDir, fileName)

            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )

            Pair(photoFile, photoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveTempPhotoAsPermanent(context: Context, tempPhotoPath: String): String? {
        return try {
            val tempFile = File(tempPhotoPath)
            if (!tempFile.exists()) return null
            val photoDir = File(context.filesDir, PHOTO_DIRECTORY)
            val fileName = "IMG_${UUID.randomUUID()}.jpg"
            val permanentFile = File(photoDir, fileName)

            tempFile.copyTo(permanentFile, overwrite = true)
            tempFile.delete()
            permanentFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deletePhoto(photoPath: String?): Boolean {
        if (photoPath.isNullOrBlank()) return false
        return try {
            val file = File(photoPath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio: Float = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        return bitmap.scale(newWidth, newHeight)
    }
}