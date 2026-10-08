package com.ahmetkaragunlu.financeai.core.media.testing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File

/** Small real JPEG shared by ViewModel photo tests; stored only in the test cache. */
class ReceiptImage(context: Context) : AutoCloseable {
    val file: File = File.createTempFile("receipt-test-", ".jpg", context.cacheDir)
    val uri: Uri get() = Uri.fromFile(file)
    init {
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        } finally { bitmap.recycle() }
    }
    override fun close() { file.delete() }
}
