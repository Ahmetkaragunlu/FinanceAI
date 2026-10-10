package com.ahmetkaragunlu.financeai.core.media.remote

import android.net.Uri
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
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
    suspend fun uploadPhoto(
        path: String,
        remoteId: String,
        recordType: PhotoRecordType,
        ownerId: String,
        version: String
    ): String {
        require(auth.currentUser?.uid == ownerId)
        require(remoteId.isNotBlank() && '/' !in remoteId)
        require(version.isNotBlank() && '/' !in version)
        val file = File(path)
        check(file.isFile)
        // Immutable version paths prevent a late upload from overwriting newer image content.
        val ref =
            storage.reference.child("users/$ownerId/${recordType.wireValue}/$remoteId/$version.jpg")
        val upload = ref.putFile(Uri.fromFile(file))
        try {
            upload.await()
        } catch (e: CancellationException) {
            upload.cancel(); throw e
        }
        require(auth.currentUser?.uid == ownerId)
        return ref.downloadUrl.await().toString()
    }

    suspend fun deletePhoto(url: String, ownerId: String) {
        require(auth.currentUser?.uid == ownerId)
        val ref = storage.getReferenceFromUrl(url)
        require(ref.path.startsWith("users/$ownerId/"))
        try {
            ref.delete().await()
        } catch (e: StorageException) {
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }
    }
}
