package com.ahmetkaragunlu.financeai.core.media.remote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import dagger.Lazy
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoRemoteCacheTest {
    @Test fun oldAccountNeverReusesPhotoAndNewVersionDoesNotOverwritePreviousFileOnFailure() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = AccountSession()
        session.activate("cache-test-owner", "USD")
        val account = session.requireAccount()
        val calls = AtomicInteger()
        val cache = PhotoRemoteCache(context, Lazy { calls.incrementAndGet(); error("offline") }, session, Dispatchers.IO)
        val file = File.createTempFile("phase2-photo-", ".jpg", context.cacheDir)
        try {
            file.writeText("retained receipt")
            val photo = RemotePhoto("https://example.test/photo", "v1")
            assertEquals(file.absolutePath, cache.prepare(account, "receipt", photo, file.absolutePath, photo))
            assertEquals(0, calls.get())
            assertNull(cache.prepare(account, "receipt", photo.copy(version = "v2"), file.absolutePath, photo))
            assertEquals("retained receipt", file.readText())
            session.deactivate(); session.activate("B", "EUR")
            assertNull(cache.prepare(account, "receipt", photo, file.absolutePath, photo))
            assertEquals(1, calls.get())
        } finally {
            file.delete()
            File(context.filesDir, "transaction_photos/cache-test-owner").delete()
        }
    }
}
