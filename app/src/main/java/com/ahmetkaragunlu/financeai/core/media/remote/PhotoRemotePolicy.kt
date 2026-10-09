package com.ahmetkaragunlu.financeai.core.media.remote

import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount

/** Shared photo rules; record persistence and reminder decisions stay with each feature. */
object PhotoRemotePolicy {
    fun normalize(data: Map<String, Any?>): Map<String, Any?> = mapOf(
        PhotoFields.STORAGE_URL to data[PhotoFields.STORAGE_URL],
        PhotoFields.REMOVED to (data[PhotoFields.REMOVED] == true),
        PhotoFields.VERSION to data[PhotoFields.VERSION],
        PhotoFields.INTENT to data[PhotoFields.INTENT],
    )

    /** Caller skips removed photos before reading local state; network work stays outside Room transactions. */
    suspend fun prepare(
        cache: PhotoRemoteCache,
        account: ActiveAccount,
        remoteId: String,
        data: Map<String, Any?>,
        existingPath: String?,
        baseline: Map<String, Any?>,
    ): Map<String, Any?> {
        val localPhoto = cache.prepare(
            account, remoteId,
            RemotePhoto(data[PhotoFields.STORAGE_URL] as? String, data[PhotoFields.VERSION] as? String),
            existingPath,
            RemotePhoto(baseline[PhotoFields.STORAGE_URL] as? String, baseline[PhotoFields.VERSION] as? String),
        )
        return data + mapOf(PhotoFields.LOCAL_URI to localPhoto)
    }

    fun select(data: Map<String, Any?>, existingPath: String?, previousUrl: Any?): String? {
        if (data[PhotoFields.REMOVED] == true) return null
        (data[PhotoFields.LOCAL_URI] as? String)?.let { return it }
        val incomingUrl = data[PhotoFields.STORAGE_URL] as? String
        val localPhoto = existingPath?.takeUnless { it.startsWith("http://") || it.startsWith("https://") }
        return if (localPhoto != null && (incomingUrl == null || incomingUrl == previousUrl)) localPhoto
        else incomingUrl ?: existingPath
    }
}
