package com.ahmetkaragunlu.financeai.photo

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

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
        firestoreId: String,
        ownerId: String
    ): Result<String> {
        return uploadPhoto(localPhotoPath, firestoreId, TRANSACTIONS_PATH, ownerId)
    }

    suspend fun uploadScheduledPhoto(
        localPhotoPath: String,
        firestoreId: String,
        ownerId: String
    ): Result<String> {
        return uploadPhoto(localPhotoPath, firestoreId, SCHEDULED_PATH, ownerId)
    }

    private suspend fun uploadPhoto(
        localPhotoPath: String,
        firestoreId: String,
        folder: String,
        ownerId: String
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))
            require(userId == ownerId) { "Stale photo owner" }
            val photoFile = File(localPhotoPath)
            if (!photoFile.exists()) {
                return Result.failure(Exception("Photo file not found"))
            }
            val storageRef = storage.reference
                .child("users")
                .child(userId)
                .child(folder)
                .child("${firestoreId}_photo.jpg")
            val fileUri = Uri.fromFile(photoFile)
            val upload = storageRef.putFile(fileUri)
            try { upload.await() }
            catch (e: CancellationException) { upload.cancel(); throw e }
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    suspend fun deleteScheduledPhotoById(scheduledFirestoreId: String, ownerId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            require(userId == ownerId) { "Stale photo owner" }
            val storageRef = storage.reference
                .child("users")
                .child(ownerId)
                .child(SCHEDULED_PATH)
                .child("${scheduledFirestoreId}_photo.jpg")

            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if ((e as? StorageException)?.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return Result.success(Unit)
            }
            Result.failure(e)
        }
    }

}
