package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.local.CameraPhotoDrafts
import com.ahmetkaragunlu.financeai.core.media.presentation.CameraCaptureLauncher
import com.ahmetkaragunlu.financeai.core.media.presentation.PhotoSourceBottomSheet
import com.ahmetkaragunlu.financeai.core.ui.effect.ToastMessageEffect
import com.ahmetkaragunlu.financeai.feature.location.presentation.MapLocationPickerRoute
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionResultEffect
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component.TransactionDatePickerDialog

@Composable
fun AddTransactionRoute(
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel(),
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var isDatePickerOpen by rememberSaveable { mutableStateOf(false) }
    var showPhotoBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }
    var cameraCaptureLauncherRef by remember { mutableStateOf<CameraCaptureLauncher?>(null) }
    // Photo Picker Launcher
    val photoPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            uri: Uri? ->
            uri?.let { viewModel.onPhotoSelected(it) }
        }
    // Camera Launcher
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
            success ->
            if (success) {
                viewModel.onCameraPhotoTaken()
            } else {
                viewModel.clearTempCameraPhoto()
            }
        }
    // Camera Permission Launcher
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            isGranted ->
            cameraCaptureLauncherRef?.onPermissionResult(isGranted)
        }
    val cameraCaptureLauncher =
        remember(context, viewModel, cameraLauncher, cameraPermissionLauncher) {
            CameraCaptureLauncher(
                    context = context,
                    cameraLauncher = cameraLauncher,
                    permissionLauncher = cameraPermissionLauncher,
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

    ToastMessageEffect(viewModel.feedbackMessageRes, viewModel::consumeFeedback)
    TransactionResultEffect(viewModel.actionResult, viewModel::consumeActionResult) { result ->
        if (result == TransactionActionResult.Saved) {
            onSaved()
            Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show()
        }
    }
    AddTransactionScreen(
        state =
            AddTransactionUiState(
                viewModel.selectedTransactionType,
                viewModel.selectedCategory,
                viewModel.inputAmount,
                viewModel.inputNote,
                viewModel.selectedDate,
                viewModel.isReminderEnabled,
                viewModel.selectedPhotoUri,
                viewModel.selectedLocation,
                viewModel.dateZone(),
            ),
        onTypeChanged = viewModel::updateTransactionType,
        onAmountChanged = viewModel::updateInputAmount,
        onCategoryChanged = viewModel::updateCategory,
        onNoteChanged = viewModel::updateInputNote,
        onReminderChanged = viewModel::toggleReminder,
        onClearLocation = viewModel::clearLocation,
        onClearPhoto = viewModel::clearPhoto,
        onDateClick = { isDatePickerOpen = true },
        onLocationClick = { showLocationPicker = true },
        onPhotoClick = { showPhotoBottomSheet = true },
        onSaveClick = viewModel::saveTransaction,
        modifier = modifier,
    )
    if (showLocationPicker) {
        MapLocationPickerRoute(
            onLocationSelected = { lat, lon ->
                viewModel.onLocationSelected(lat, lon)
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false },
        )
    }

    // Photo Source Bottom Sheet
    if (showPhotoBottomSheet) {
        PhotoSourceBottomSheet(
            onDismiss = { showPhotoBottomSheet = false },
            onCameraClick = { cameraCaptureLauncher.launchCamera() },
            onGalleryClick = { photoPickerLauncher.launch("image/*") },
        )
    }

    if (isDatePickerOpen) {
        TransactionDatePickerDialog(
            initialSelectedDateMillis = viewModel.pickerDate(),
            isDateValid = viewModel::isPickerDateValid,
            onDateSelected = { timestamp ->
                viewModel.selectPickerDate(timestamp)
                isDatePickerOpen = false
            },
            onDismiss = { isDatePickerOpen = false },
        )
    }
}
