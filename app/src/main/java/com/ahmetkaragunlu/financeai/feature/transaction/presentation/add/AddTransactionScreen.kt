package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.format.*
import com.ahmetkaragunlu.financeai.core.ui.component.EditTextField
import com.ahmetkaragunlu.financeai.core.ui.component.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.core.ui.component.getAccountCurrencySymbol
import com.ahmetkaragunlu.financeai.core.ui.effect.ToastMessageEffect
import com.ahmetkaragunlu.financeai.core.ui.theme.AddTransactionScreenTextFieldStyles
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import com.ahmetkaragunlu.financeai.feature.location.presentation.MapLocationPickerRoute
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.format.toResId
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionResultEffect
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.*
import com.ahmetkaragunlu.financeai.photo.CameraHelper
import com.ahmetkaragunlu.financeai.photo.PhotoSourceBottomSheet
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil

@OptIn(ExperimentalMaterial3Api::class)
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
    var cameraHelperRef by remember { mutableStateOf<CameraHelper?>(null) }
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
            cameraHelperRef?.onPermissionResult(isGranted)
        }
    val cameraHelper =
        remember(context, viewModel, cameraLauncher, cameraPermissionLauncher) {
            CameraHelper(
                    context = context,
                    cameraLauncher = cameraLauncher,
                    permissionLauncher = cameraPermissionLauncher,
                    onPreparePhoto = {
                        viewModel.cameraOwnerId()?.let { owner ->
                            PhotoStorageUtil.createTempPhotoFile(context, owner)?.also { (file, _)
                                ->
                                viewModel.registerCameraDraft(file.absolutePath)
                            }
                        }
                    },
                )
                .also { cameraHelperRef = it }
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
            TransactionDraftState(
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
            onCameraClick = { cameraHelper.launchCamera() },
            onGalleryClick = { photoPickerLauncher.launch("image/*") },
        )
    }

    // Date Picker Dialog
    if (isDatePickerOpen) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = viewModel.pickerDate(),
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            return viewModel.isPickerDateValid(utcTimeMillis)
                        }
                    },
            )

        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            colors = DatePickerDefaults.colors(containerColor = FinanceColors.dialogSurface),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            if (viewModel.isPickerDateValid(timestamp)) {
                                viewModel.selectPickerDate(timestamp)
                                isDatePickerOpen = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerOpen = false }) {
                    Text(
                        stringResource(id = R.string.cancel),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                colors =
                    DatePickerDefaults.colors(
                        containerColor = FinanceColors.dialogSurface,
                        dayContentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledDayContentColor = FinanceColors.mutedText,
                        weekdayContentColor = MaterialTheme.colorScheme.onPrimary,
                        dividerColor = FinanceColors.dialogSurface,
                        navigationContentColor = MaterialTheme.colorScheme.onPrimary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        headlineContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedDayContainerColor = FinanceColors.mutedText,
                        todayDateBorderColor = FinanceColors.mutedText,
                        todayContentColor = FinanceColors.mutedText,
                    ),
            )
        }
    }
}

@Composable
fun AddTransactionScreen(
    state: TransactionDraftState,
    onTypeChanged: (TransactionType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategoryChanged: (CategoryType) -> Unit,
    onNoteChanged: (String) -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onClearLocation: () -> Unit,
    onClearPhoto: () -> Unit,
    onDateClick: () -> Unit,
    onLocationClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .fillMaxSize()
                .background(color = colorResource(R.color.background))
                .verticalScroll(rememberScrollState()),
    ) {
        // Transaction Type Selection
        TransactionTypeSelector(state.type, onTypeChanged)
        Spacer(modifier = modifier.height(Spacing.sectionGap))

        // Form Fields
        Column(
            modifier = modifier.padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TransactionInputFields(
                state.amount,
                state.note,
                state.category,
                state.type,
                onAmountChanged,
                onNoteChanged,
                onCategoryChanged,
            )

            // Date Picker
            DatePickerField(
                selectedDate = state.date,
                onDateClick = { onDateClick() },
                isRemenderEnabled = state.reminderEnabled,
                zone = state.zone,
                modifier = modifier.widthIn(max = 450.dp).padding(bottom = 14.dp).fillMaxWidth(),
            )

            // Reminder Switch
            ReminderSwitch(
                isEnabled = state.reminderEnabled,
                onToggle = onReminderChanged,
                modifier = modifier.widthIn(max = 450.dp).padding(bottom = 16.dp).fillMaxWidth(),
            )

            // Location & Photo Cards
            TransactionAttachments(
                state.location,
                state.photoUri,
                onLocationClick,
                onPhotoClick,
                onClearLocation,
                onClearPhoto,
            )
            // Save Button
            Button(
                onClick = onSaveClick,
                modifier = modifier.widthIn(max = 450.dp).fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(
                        id =
                            if (state.reminderEnabled) R.string.create_reminder_button
                            else R.string.save_button
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    type: TransactionType,
    onTypeChanged: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(Spacing.screenPadding).widthIn(max = 400.dp).fillMaxWidth()) {
        OutlinedButton(
            onClick = { onTypeChanged(TransactionType.EXPENSE) },
            modifier = modifier.weight(1f),
            colors =
                if (type == TransactionType.EXPENSE) {
                    ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface)
                } else ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(
                text = stringResource(R.string.expense),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = modifier.width(16.dp))
        OutlinedButton(
            onClick = { onTypeChanged(TransactionType.INCOME) },
            modifier = Modifier.weight(1f),
            colors =
                if (type == TransactionType.INCOME) {
                    ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface)
                } else ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(
                text = stringResource(R.string.income),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun TransactionInputFields(
    amount: String,
    note: String,
    category: CategoryType?,
    type: TransactionType,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategoryChanged: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCategoryDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    // Amount Field
    EditTextField(
        value = amount,
        onValueChange = onAmountChanged,
        modifier = modifier.widthIn(max = 450.dp).padding(bottom = 16.dp).fillMaxWidth(),
        keyboardOptions =
            KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number,
            ),
        placeholder = R.string.enter_amount,
        colors = AddTransactionScreenTextFieldStyles.textFieldColors(),
        trailingIcon = {
            Text(getAccountCurrencySymbol(), color = MaterialTheme.colorScheme.onPrimary)
        },
    )

    // Category Dropdown
    FinanceDropdownMenu(
        modifier = modifier.widthIn(max = 450.dp).padding(bottom = 14.dp).fillMaxWidth(),
        expanded = isCategoryDropdownExpanded,
        onExpandedChange = { isOpen -> isCategoryDropdownExpanded = isOpen },
        options = CategoryType.entries.filter { it.type == type },
        onOptionSelected = onCategoryChanged,
        itemLabel = { category -> stringResource(category.toResId()) },
        trigger = {
            OutlinedTextField(
                value = category?.let { stringResource(it.toResId()) } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.select_category),
                        color = FinanceColors.mutedText,
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier =
                            Modifier.clickable {
                                isCategoryDropdownExpanded = !isCategoryDropdownExpanded
                            },
                    )
                },
                modifier =
                    Modifier.fillMaxWidth().clickable {
                        isCategoryDropdownExpanded = !isCategoryDropdownExpanded
                    },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = FinanceColors.fieldSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
            )
        },
    )
    // Note Field
    EditTextField(
        value = note,
        onValueChange = onNoteChanged,
        keyboardOptions =
            KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
        placeholder = R.string.enter_your_note,
        modifier = modifier.widthIn(max = 450.dp).padding(bottom = 14.dp).fillMaxWidth(),
        colors = AddTransactionScreenTextFieldStyles.textFieldColors(),
    )
}

@Composable
private fun TransactionAttachments(
    location: LocationData?,
    photoUri: Uri?,
    onLocationClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onClearLocation: () -> Unit,
    onClearPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.widthIn(max = 450.dp).fillMaxWidth().padding(bottom = 16.dp)) {
        // Location Card
        Card(
            onClick = { onLocationClick() },
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(FinanceColors.fieldSurface)) {
                if (location == null) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = FinanceColors.mutedText,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.location_optional),
                            color = FinanceColors.mutedText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp).size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = location!!.addressShort,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onClearLocation() },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove_photo),
                                tint = FinanceColors.mutedText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = modifier.width(8.dp))

        // Photo Card
        Card(
            onClick = {
                if (photoUri == null) {
                    onPhotoClick()
                }
            },
            modifier = modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(modifier = modifier.fillMaxSize().background(FinanceColors.fieldSurface)) {
                if (photoUri == null) {
                    Row(
                        modifier = modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = FinanceColors.mutedText,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                        Spacer(modifier = modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.photo_optional),
                            color = FinanceColors.mutedText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = stringResource(R.string.selected_photo),
                        modifier = modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    IconButton(
                        onClick = { onClearPhoto() },
                        modifier =
                            modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .padding(2.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(50)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.remove_photo),
                            tint = FinanceColors.onAccent,
                            modifier = modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
