package com.ahmetkaragunlu.financeai.core.media

import org.junit.Assert.*
import org.junit.Test

class PhotoDimensionsTest {
    @Test fun receiptAspectRatioIsPreservedAndSmallImagesAreNotEnlarged() {
        assertEquals(1920 to 640, PhotoDimensions.target(6000, 2000))
        assertEquals(640 to 1920, PhotoDimensions.target(2000, 6000))
        assertEquals(320 to 100, PhotoDimensions.target(320, 100))
    }
    @Test fun extremeAspectRatioNeverProducesZeroPixels() {
        assertEquals(1920 to 1, PhotoDimensions.target(100000, 1))
    }
    @Test(expected = IllegalArgumentException::class) fun invalidHeaderDimensionsAreRejected() {
        PhotoDimensions.target(0, 100)
    }
}
