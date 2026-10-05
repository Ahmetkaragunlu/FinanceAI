package com.ahmetkaragunlu.financeai.core.media

import android.content.Context
import android.net.Uri
import com.ahmetkaragunlu.financeai.core.coroutines.di.IoDispatcher
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class PhotoLocalStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    suspend fun save(uri: Uri, cameraPath: String?, ownerId: String): String? = withContext(dispatcher) {
        if (cameraPath != null) PhotoStorageUtil.saveTempPhotoAsPermanent(context, cameraPath, ownerId)
        else PhotoStorageUtil.savePhotoToInternalStorage(context, uri, ownerId)
    }
    suspend fun delete(path: String?) = withContext(dispatcher) { PhotoStorageUtil.deletePhoto(path) }
}
