package com.ahmetkaragunlu.financeai.core.media.remote

import android.content.Context
import com.ahmetkaragunlu.financeai.core.coroutines.di.IoDispatcher
import com.ahmetkaragunlu.financeai.core.media.local.PhotoFiles
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.google.firebase.storage.FirebaseStorage
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class RemotePhoto(val url: String?, val version: String?)

/** Network/file preparation happens outside the Room transaction. Cache files remain account-owned. */
class PhotoRemoteCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: Lazy<FirebaseStorage>,
    private val session: AccountSession,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    suspend fun prepare(account: ActiveAccount, remoteId: String, photo: RemotePhoto, existingPath: String?, previous: RemotePhoto?): String? = withContext(io) {
        val url = photo.url
        if (!session.isCurrent(account) || url.isNullOrBlank()) return@withContext null
        if (photo == previous && existingPath != null && File(existingPath).isFile) return@withContext existingPath
        val hash = MessageDigest.getInstance("SHA-256").digest("$url|${photo.version}".toByteArray()).take(12).joinToString("") { "%02x".format(it) }
        val folder = File(context.filesDir, "${PhotoFiles.DIRECTORY}/${account.ownerId}")
        val file = File(folder, "${PhotoFiles.CACHE_PREFIX}${remoteId.hashCode()}_$hash.jpg")
        try {
            if (!file.isFile) {
                check(folder.isDirectory || folder.mkdirs())
                val temporary = File.createTempFile(PhotoFiles.CACHE_PREFIX, ".part", folder)
                try {
                    val download = storage.get().getReferenceFromUrl(url).getFile(temporary)
                    try { download.await() }
                    catch (e: CancellationException) { download.cancel(); throw e }
                    check(temporary.renameTo(file))
                } finally { temporary.delete() }
            }
            if (session.isCurrent(account)) file.absolutePath else null
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
    }

    companion object {
        fun isCachedPath(path: String): Boolean = File(path).name.startsWith(PhotoFiles.CACHE_PREFIX)
    }
}
