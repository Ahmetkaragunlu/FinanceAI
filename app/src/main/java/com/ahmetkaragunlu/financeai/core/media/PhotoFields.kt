package com.ahmetkaragunlu.financeai.core.media

object PhotoFields {
    const val STORAGE_URL = "photoStorageUrl"
    const val REMOVED = "photoRemoved"
    const val VERSION = "photoVersion"
    const val INTENT = "photoIntent"
    // Deliberate persisted allowlist, ordered for completed-plan edits. LOCAL_URI is never included.
    val PERSISTED_METADATA: Set<String> = setOf(STORAGE_URL, REMOVED, VERSION, INTENT)
    // Internal prepared payload key; not a remotely persisted URL.
    const val LOCAL_URI = "localPhotoUri"
}
