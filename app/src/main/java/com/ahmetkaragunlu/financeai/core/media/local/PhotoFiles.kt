package com.ahmetkaragunlu.financeai.core.media.local

/** Stable app-private path shared by media storage, camera drafts and backup. */
object PhotoFiles {
    const val DIRECTORY = "transaction_photos"
    const val PERMANENT_PREFIX = "IMG_"
    const val CACHE_PREFIX = "SYNC_"
    const val CAMERA_PREFIX = "TEMP_"
    const val PREPARATION_PREFIX = "PREP_"

    fun requireOwnerId(ownerId: String) {
        require(ownerId.isNotBlank() && '/' !in ownerId && ownerId != "." && ownerId != "..")
    }

    fun isPermanentOrCache(name: String): Boolean =
        name.startsWith(PERMANENT_PREFIX) || name.startsWith(CACHE_PREFIX)
}
