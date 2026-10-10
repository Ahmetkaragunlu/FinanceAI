package com.ahmetkaragunlu.financeai.app.backup

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceBackupPhotosTest {
    private fun withPhotos(test: (File, File, File) -> Unit) {
        val root = Files.createTempDirectory("financeai-backup-photos-").toFile()
        val photos = File(root, "transaction_photos").apply { mkdirs() }
        val owner = File(photos, "synthetic-owner").apply { mkdirs() }
        try {
            test(root, photos, owner)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun backupIncludesOnlyPermanentAndCacheJpegsAtTheOriginalOwnerDepth() =
        withPhotos { _, photos, owner ->
            for (name in listOf("IMG_receipt.jpg", "SYNC_receipt.jpg")) {
                val file = File(owner, name).apply { writeText("synthetic image") }
                assertTrue(isBackupPhoto(file, owner, photos))
            }
            for (name in listOf(
                "TEMP_camera.jpg",
                "PREP_decode.jpg",
                "IMG_receipt.png",
                "IMG_upper.JPG",
                "unknown.jpg"
            )) {
                val file = File(owner, name).apply { writeText("synthetic image") }
                assertFalse(isBackupPhoto(file, owner, photos))
            }
            val directory = File(owner, "IMG_folder.jpg").apply { mkdirs() }
            assertFalse(isBackupPhoto(directory, owner, photos))
        }

    @Test
    fun flatNestedAndCanonicalEscapingPhotosRemainExcluded() = withPhotos { root, photos, owner ->
        val flat = File(photos, "IMG_flat.jpg").apply { writeText("flat") }
        assertFalse(isBackupPhoto(flat, owner, photos))
        val nestedOwner = File(owner, "nested").apply { mkdirs() }
        val nested = File(nestedOwner, "IMG_nested.jpg").apply { writeText("nested") }
        assertFalse(isBackupPhoto(nested, nestedOwner, photos))
        val outside = File(root, "outside").apply { mkdirs() }
        val outsideFile = File(outside, "IMG_outside.jpg").apply { writeText("outside") }
        val linkedFile = File(owner, "IMG_link.jpg")
        Files.createSymbolicLink(linkedFile.toPath(), outsideFile.toPath())
        assertFalse(isBackupPhoto(linkedFile, owner, photos))
        val linkedOwner = File(photos, "linked-owner")
        Files.createSymbolicLink(linkedOwner.toPath(), outside.toPath())
        assertFalse(isBackupPhoto(File(linkedOwner, outsideFile.name), linkedOwner, photos))
    }
}
