package com.ahmetkaragunlu.financeai.core.media

import kotlin.math.roundToInt

object PhotoDimensions {
    const val MAX_EDGE = 1920
    const val JPEG_QUALITY = 85
    fun target(width: Int, height: Int): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val ratio = minOf(1.0, MAX_EDGE.toDouble() / maxOf(width, height))
        return (width * ratio).roundToInt().coerceAtLeast(1) to (height * ratio).roundToInt().coerceAtLeast(1)
    }
}
