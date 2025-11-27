package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat
import com.ahmetkaragunlu.financeai.R
import java.io.File

// ARTIK BAĞIMSIZ: onPreparePhoto parametresi ile her yerden kullanılabilir.
class CameraHelper(
    private val context: Context,
    private val cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    private val onPreparePhoto: () -> Pair<File, Uri>?
) {
    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            openCamera()
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun openCamera() {
        try {
            // ViewModel'dan gelen hazırlama fonksiyonunu çağırır
            val cameraPhotoData = onPreparePhoto()
            cameraPhotoData?.let { (_, uri) ->
                cameraLauncher.launch(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.error_camera_failed, e.localizedMessage ?: ""),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.error_camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}