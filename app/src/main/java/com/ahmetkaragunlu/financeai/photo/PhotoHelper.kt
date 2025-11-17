
package com.ahmetkaragunlu.financeai.photo


import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.viewmodel.AddTransactionViewModel

class CameraHelper(
    private val context: Context,
    private val viewModel: AddTransactionViewModel,
    private val cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
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
            val cameraPhotoData = viewModel.prepareCameraPhoto()
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