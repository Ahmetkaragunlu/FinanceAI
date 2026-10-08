package com.ahmetkaragunlu.financeai.core.media.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat
import com.ahmetkaragunlu.financeai.R
import java.io.File
import kotlinx.coroutines.CancellationException

class CameraHelper(
    private val context: Context,
    private val cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    private val onPreparePhoto: () -> Pair<File, Uri>?
) {
    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun openCamera() {
        try {
            val cameraPhotoData = onPreparePhoto()
            cameraPhotoData?.let { (_, uri) ->
                cameraLauncher.launch(uri)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Toast.makeText(
                context,
                context.getString(R.string.photo_capture_failed),
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
