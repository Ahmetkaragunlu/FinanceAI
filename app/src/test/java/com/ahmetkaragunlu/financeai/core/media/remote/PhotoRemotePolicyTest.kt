package com.ahmetkaragunlu.financeai.core.media.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoRemotePolicyTest {
    @Test
    fun removalOverridesPreparedAndRetainedPhotoPaths() {
        val data = mapOf(
            "photoRemoved" to true, "photoStorageUrl" to "https://new",
            "localPhotoUri" to "/prepared.jpg"
        )
        assertNull(PhotoRemotePolicy.select(data, "/existing.jpg", "https://old"))
    }

    @Test
    fun preparedPathsWinIncludingThePreviouslyAcceptedEmptyString() {
        for (prepared in listOf("/prepared.jpg", "")) {
            val data = mapOf("localPhotoUri" to prepared, "photoStorageUrl" to "https://new")
            assertEquals(prepared, PhotoRemotePolicy.select(data, "/existing.jpg", "https://old"))
        }
    }

    @Test
    fun unchangedOrMissingRemoteUrlKeepsALocalPhotoButChangedUrlDoesNot() {
        assertEquals(
            "/local.jpg",
            PhotoRemotePolicy.select(emptyMap(), "/local.jpg", "https://old")
        )
        assertEquals(
            "/local.jpg",
            PhotoRemotePolicy.select(
                mapOf("photoStorageUrl" to "https://old"),
                "/local.jpg",
                "https://old"
            )
        )
        assertEquals(
            "https://new",
            PhotoRemotePolicy.select(
                mapOf("photoStorageUrl" to "https://new"),
                "/local.jpg",
                "https://old"
            )
        )
        assertEquals(
            "42",
            PhotoRemotePolicy.select(mapOf("photoStorageUrl" to "42"), "/local.jpg", 42L)
        )
    }

    @Test
    fun remoteFallbackAndExistingHttpRecognitionKeepTheirOriginalMeaning() {
        assertEquals("https://old", PhotoRemotePolicy.select(emptyMap(), "https://old", null))
        assertEquals("http://old", PhotoRemotePolicy.select(emptyMap(), "http://old", null))
        assertNull(PhotoRemotePolicy.select(emptyMap(), null, "https://old"))
        assertEquals(
            "http-like-local.jpg", PhotoRemotePolicy.select(
                mapOf("photoStorageUrl" to "https://old"), "http-like-local.jpg", "https://old"
            )
        )
    }

    @Test
    fun normalizationKeepsTheWireValuesAndOnlyBooleanTrueMeansRemoved() {
        val data = mapOf(
            "photoStorageUrl" to 42L, "photoRemoved" to "true", "photoVersion" to 7,
            "photoIntent" to "intent", "localPhotoUri" to "/private.jpg", "unrelated" to true
        )
        assertEquals(
            mapOf(
                "photoStorageUrl" to 42L, "photoRemoved" to false,
                "photoVersion" to 7, "photoIntent" to "intent"
            ), PhotoRemotePolicy.normalize(data)
        )
        assertFalse(PhotoRemotePolicy.normalize(data).containsKey("localPhotoUri"))
        assertEquals(
            mapOf(
                "photoStorageUrl" to null, "photoRemoved" to false,
                "photoVersion" to null, "photoIntent" to null
            ), PhotoRemotePolicy.normalize(emptyMap())
        )
    }
}
