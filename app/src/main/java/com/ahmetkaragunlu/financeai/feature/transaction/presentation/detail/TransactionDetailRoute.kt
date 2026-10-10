package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.core.media.local.CameraPhotoDrafts
import com.ahmetkaragunlu.financeai.core.media.presentation.CameraCaptureLauncher
import com.ahmetkaragunlu.financeai.core.ui.effect.ToastMessageEffect

@Composable
fun TransactionDetailRoute(
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val detailState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var cameraCaptureLauncherRef by remember { mutableStateOf<CameraCaptureLauncher?>(null) }
    ToastMessageEffect(viewModel.photoErrorResId, viewModel::consumePhotoError)
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
            success ->
            if (success) {
                viewModel.onCameraPhotoTaken()
            } else viewModel.clearCameraDraft()
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            isGranted ->
            cameraCaptureLauncherRef?.onPermissionResult(isGranted)
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.onPhotoSelected(it) }
        }

    val cameraCaptureLauncher =
        remember(context, cameraLauncher, permissionLauncher) {
            CameraCaptureLauncher(
                    context = context,
                    cameraLauncher = cameraLauncher,
                    permissionLauncher = permissionLauncher,
                    onPreparePhoto = {
                        viewModel.cameraOwnerId()?.let { owner ->
                            CameraPhotoDrafts.createTempPhotoFile(context, owner)?.also { (file, _)
                                ->
                                viewModel.registerCameraDraft(file.absolutePath)
                            }
                        }
                    },
                )
                .also { cameraCaptureLauncherRef = it }
        }

    TransactionDetailScreen(
        state = detailState,
        editState =
            TransactionEditState(
                viewModel.editAmount,
                viewModel.editNote,
                viewModel.editCategory,
                viewModel.availableCategories,
            ),
        onEditRequested = viewModel::prepareEdit,
        onAmountChanged = viewModel::updateEditAmount,
        onNoteChanged = viewModel::updateEditNote,
        onCategoryChanged = viewModel::updateEditCategory,
        onUpdateRequested = viewModel::updateTransaction,
        onDeleteRequested = viewModel::deleteTransaction,
        actionResult = viewModel.actionResult,
        onResultConsumed = viewModel::consumeActionResult,
        onDeleted = onDeleted,
        onDeletePhoto = viewModel::deletePhoto,
        onCameraClick = cameraCaptureLauncher::launchCamera,
        onGalleryClick = { galleryLauncher.launch("image/*") },
        modifier = modifier,
    )
}
