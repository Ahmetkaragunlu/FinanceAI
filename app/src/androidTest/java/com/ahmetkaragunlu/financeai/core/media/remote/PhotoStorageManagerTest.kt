package com.ahmetkaragunlu.financeai.core.media.remote

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.testing.ReceiptImage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class PhotoStorageManagerTest {
    private class Fixture {
        val auth = mock(FirebaseAuth::class.java)
        val user = mock(FirebaseUser::class.java)
        val storage = mock(FirebaseStorage::class.java)
        val root = mock(StorageReference::class.java)
        val target = mock(StorageReference::class.java)
        val upload = mock(UploadTask::class.java)
        var owner = "A"
        var afterUpload: () -> Unit = {}
        val manager = PhotoStorageManager(storage, auth)

        init {
            `when`(auth.currentUser).thenReturn(user)
            `when`(user.uid).thenAnswer { owner }
            `when`(storage.reference).thenReturn(root)
            `when`(root.child("users/A/transactions/record/version.jpg")).thenReturn(target)
            `when`(root.child("users/A/scheduled/record/version.jpg")).thenReturn(target)
            `when`(target.putFile(any())).thenReturn(upload)
            `when`(upload.isComplete).thenAnswer { afterUpload(); true }
            `when`(target.downloadUrl).thenReturn(Tasks.forResult(Uri.parse("https://example.test/photo")))
        }
    }

    @Test fun uploadKeepsBothImmutableVersionPathsAndCannotReturnAFormerAccountsUrl(): Unit = runBlocking {
        val f = Fixture()
        ReceiptImage(ApplicationProvider.getApplicationContext<Context>()).use { image ->
            assertEquals("https://example.test/photo", f.manager.uploadPhoto(image.file.path, "record", PhotoRecordType.TRANSACTION, "A", "version"))
            assertEquals("https://example.test/photo", f.manager.uploadPhoto(image.file.path, "record", PhotoRecordType.SCHEDULED, "A", "version"))
            verify(f.root).child("users/A/transactions/record/version.jpg")
            verify(f.root).child("users/A/scheduled/record/version.jpg")
            f.afterUpload = { f.owner = "B" }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { f.manager.uploadPhoto(image.file.path, "record", PhotoRecordType.TRANSACTION, "A", "version") }
            }
        }
    }

    @Test fun invalidOwnerRecordAndVersionCannotStartAnUpload(): Unit = runBlocking {
        val f = Fixture()
        f.owner = "B"
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { f.manager.uploadPhoto("/test.jpg", "record", PhotoRecordType.TRANSACTION, "A", "version") }
        }
        f.owner = "A"
        for ((record, version) in listOf("" to "version", "../record" to "version", "record" to "", "record" to "../version")) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { f.manager.uploadPhoto("/test.jpg", record, PhotoRecordType.TRANSACTION, "A", version) }
            }
        }
        verify(f.target, never()).putFile(any())
    }

    @Test fun deletionRejectsForeignPathsAndIgnoresOnlyAlreadyMissingObjects(): Unit = runBlocking {
        val f = Fixture()
        val url = "https://example.test/photo"
        `when`(f.storage.getReferenceFromUrl(url)).thenReturn(f.target)
        `when`(f.target.path).thenReturn("users/B/transactions/record/version.jpg")
        assertThrows(IllegalArgumentException::class.java) { runBlocking { f.manager.deletePhoto(url, "A") } }
        verify(f.target, never()).delete()
        `when`(f.target.path).thenReturn("users/A/transactions/record/version.jpg")
        val missing = mock(StorageException::class.java)
        `when`(missing.errorCode).thenReturn(StorageException.ERROR_OBJECT_NOT_FOUND)
        `when`(f.target.delete()).thenReturn(Tasks.forException(missing))
        f.manager.deletePhoto(url, "A")
        val error = IOException("synthetic offline")
        `when`(f.target.delete()).thenReturn(Tasks.forException(error))
        assertSame(error, assertThrows(IOException::class.java) { runBlocking { f.manager.deletePhoto(url, "A") } })
    }
}
