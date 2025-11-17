package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoStorageManager @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TRANSACTIONS_PATH = "transactions"
        private const val SCHEDULED_PATH = "scheduled"
    }

    suspend fun uploadTransactionPhoto(
        localPhotoPath: String,
        firestoreId: String
    ): Result<String> {
        return uploadPhoto(localPhotoPath, firestoreId, TRANSACTIONS_PATH)
    }

    suspend fun uploadScheduledPhoto(
        localPhotoPath: String,
        firestoreId: String
    ): Result<String> {
        return uploadPhoto(localPhotoPath, firestoreId, SCHEDULED_PATH)
    }

    private suspend fun uploadPhoto(
        localPhotoPath: String,
        firestoreId: String,
        folder: String
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))
            val photoFile = File(localPhotoPath)
            if (!photoFile.exists()) {
                return Result.failure(Exception("Photo file not found: $localPhotoPath"))
            }
            val storageRef = storage.reference
                .child("users")
                .child(userId)
                .child(folder)
                .child("${firestoreId}_photo.jpg")
            val fileUri = Uri.fromFile(photoFile)
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndSavePhoto(
        context: Context,
        storageUrl: String,
        firestoreId: String
    ): Result<String> {
        return try {
            val storageRef = storage.getReferenceFromUrl(storageUrl)
            val photoDir = File(context.filesDir, PhotoStorageUtil.PHOTO_DIRECTORY)
            if (!photoDir.exists()) photoDir.mkdirs()
            val localFile = File(photoDir, "SYNC_${firestoreId}_${System.currentTimeMillis()}.jpg")
            storageRef.getFile(localFile).await()
            Result.success(localFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhoto(storageUrl: String): Result<Unit> {
        return try {
            if (storageUrl.isBlank()) {
                return Result.success(Unit)
            }
            val storageRef = storage.getReferenceFromUrl(storageUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun deleteScheduledPhotoById(scheduledFirestoreId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            val storageRef = storage.reference
                .child("users")
                .child(userId)
                .child(SCHEDULED_PATH)
                .child("${scheduledFirestoreId}_photo.jpg")

            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if ((e as? StorageException)?.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return Result.success(Unit)
            }
            Result.failure(e)
        }
    }

}