package com.ahmetkaragunlu.financeai.photo


import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Storage ile fotoğraf senkronizasyonu yöneticisi
 * - Her kullanıcı kendi klasöründe fotoğraf saklar
 * - Transaction ve Scheduled için ayrı klasörler
 * - Local + Cloud hybrid yaklaşım (First-Offline)
 *
 * KULLANIM:
 * 1. Kullanıcı fotoğraf ekler → Local'e kaydet (PhotoStorageUtil)
 * 2. Room'a kaydet (local path ile)
 * 3. Firebase sync'te → uploadTransactionPhoto() ile Storage'a yükle
 * 4. Firestore'da Storage URL'i sakla
 * 5. Diğer cihazda listener → downloadAndSavePhoto() ile local'e kaydet
 */
@Singleton
class PhotoStorageManager @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "PhotoStorageManager"
        private const val TRANSACTIONS_PATH = "transactions"
        private const val SCHEDULED_PATH = "scheduled"
    }

    /**
     * Transaction fotoğrafını Storage'a yükler
     * @param localPhotoPath Local'deki fotoğraf yolu
     * @param firestoreId Transaction'ın Firestore ID'si
     * @return Storage download URL veya null
     */
    suspend fun uploadTransactionPhoto(
        localPhotoPath: String,
        firestoreId: String
    ): Result<String> {
        return uploadPhoto(localPhotoPath, firestoreId, TRANSACTIONS_PATH)
    }

    /**
     * Scheduled transaction fotoğrafını Storage'a yükler
     */
    suspend fun uploadScheduledPhoto(
        localPhotoPath: String,
        firestoreId: String
    ): Result<String> {
        return uploadPhoto(localPhotoPath, firestoreId, SCHEDULED_PATH)
    }

    /**
     * Genel fotoğraf yükleme fonksiyonu
     */
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

            // Storage path: users/{userId}/{folder}/{firestoreId}_photo.jpg
            val storageRef = storage.reference
                .child("users")
                .child(userId)
                .child(folder)
                .child("${firestoreId}_photo.jpg")

            Log.d(TAG, "Uploading photo to: ${storageRef.path}")

            // Fotoğrafı yükle
            val uploadTask = storageRef.putFile(Uri.fromFile(photoFile)).await()

            // Download URL'i al
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Log.d(TAG, "Photo uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading photo", e)
            Result.failure(e)
        }
    }

    /**
     * Storage'dan fotoğrafı indirir ve local'e kaydeder
     * @param storageUrl Firebase Storage URL'i
     * @param context Android Context
     * @param firestoreId Transaction ID (benzersiz dosya adı için)
     * @return Local dosya yolu veya null
     */
    suspend fun downloadAndSavePhoto(
        context: Context,
        storageUrl: String,
        firestoreId: String
    ): Result<String> {
        return try {
            Log.d(TAG, "Downloading photo from: $storageUrl")

            // Storage reference'ı URL'den al
            val storageRef = storage.getReferenceFromUrl(storageUrl)

            // Local dosya oluştur
            val photoDir = File(context.filesDir, PhotoStorageUtil.PHOTO_DIRECTORY)
            if (!photoDir.exists()) photoDir.mkdirs()

            val localFile = File(photoDir, "SYNC_${firestoreId}_${System.currentTimeMillis()}.jpg")

            // Fotoğrafı indir
            storageRef.getFile(localFile).await()

            Log.d(TAG, "Photo downloaded to: ${localFile.absolutePath}")
            Result.success(localFile.absolutePath)

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading photo", e)
            Result.failure(e)
        }
    }

    /**
     * Storage'dan fotoğrafı siler
     */
    suspend fun deletePhoto(storageUrl: String): Result<Unit> {
        return try {
            if (storageUrl.isBlank()) {
                return Result.success(Unit)
            }

            val storageRef = storage.getReferenceFromUrl(storageUrl)
            storageRef.delete().await()

            Log.d(TAG, "Photo deleted from Storage: $storageUrl")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting photo from Storage", e)
            Result.failure(e)
        }
    }

    /**
     * Scheduled transaction'ı Transaction'a dönüştürürken
     * Storage'daki fotoğrafı taşır (scheduled/ → transactions/)
     */
    suspend fun moveScheduledPhotoToTransaction(
        scheduledFirestoreId: String,
        transactionFirestoreId: String
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val oldRef = storage.reference
                .child("users/$userId/$SCHEDULED_PATH/${scheduledFirestoreId}_photo.jpg")

            val newRef = storage.reference
                .child("users/$userId/$TRANSACTIONS_PATH/${transactionFirestoreId}_photo.jpg")

            // Metadata'yı al
            val metadata = oldRef.metadata.await()

            // Yeni lokasyona kopyala
            val bytes = oldRef.getBytes(Long.MAX_VALUE).await()
            newRef.putBytes(bytes, metadata).await()

            // Eski fotoğrafı sil
            oldRef.delete().await()

            // Yeni URL'i döndür
            val newUrl = newRef.downloadUrl.await().toString()

            Log.d(TAG, "Photo moved from scheduled to transaction")
            Result.success(newUrl)

        } catch (e: Exception) {
            Log.e(TAG, "Error moving photo", e)
            Result.failure(e)
        }
    }
}