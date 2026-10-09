package com.ahmetkaragunlu.financeai.core.media.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoFilesTest {
    @Test fun ownerValidationRejectsTheSameTraversalShapesWithoutTighteningAcceptedIds() {
        for (owner in listOf("", " ", "\n", ".", "..", "../other", "other/record", "/absolute")) {
            assertThrows(owner, IllegalArgumentException::class.java) { PhotoFiles.requireOwnerId(owner) }
        }
        for (owner in listOf("owner", "owner_with-dash", "owner..suffix", " owner ", "owner\\suffix")) {
            PhotoFiles.requireOwnerId(owner)
        }
    }

    @Test fun permanentAndCacheShapesDoNotIncludeCameraPreparationOrUnknownFiles() {
        for (name in listOf("IMG_receipt.jpg", "SYNC_receipt.jpg", "IMG_legacy.png", "SYNC_download.part")) {
            assertTrue(name, PhotoFiles.isPermanentOrCache(name))
        }
        for (name in listOf("TEMP_camera.jpg", "PREP_decode.part", "other.jpg", "img_lowercase.jpg")) {
            assertFalse(name, PhotoFiles.isPermanentOrCache(name))
        }
    }
}
