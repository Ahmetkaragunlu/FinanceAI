package com.ahmetkaragunlu.financeai.core.media.local

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Only the narrow route-owned camera draft/file-provider bridge lives here. */
object CameraPhotoDrafts {
    fun createTempPhotoFile(context: Context, ownerId: String): Pair<File, Uri>? = try {
        PhotoFiles.requireOwnerId(ownerId)
        val folder = File(context.filesDir, "${PhotoFiles.DIRECTORY}/$ownerId")
        check(folder.isDirectory || folder.mkdirs())
        val file = File(folder, "${PhotoFiles.CAMERA_PREFIX}${UUID.randomUUID()}.jpg")
        check(file.createNewFile())
        file to FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        Log.w("CameraDraft", "Could not prepare file (${e.javaClass.simpleName})")
        null
    }
}
