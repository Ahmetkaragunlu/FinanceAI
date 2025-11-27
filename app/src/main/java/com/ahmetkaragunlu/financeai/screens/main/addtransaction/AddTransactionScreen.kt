package com.ahmetkaragunlu.financeai.screens.main.addtransaction

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.location.MapLocationPickerScreen
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.photo.CameraHelper
import com.ahmetkaragunlu.financeai.photo.PhotoSourceBottomSheet
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.ui.theme.AddTransactionScreenTextFieldStyles
import com.ahmetkaragunlu.financeai.utils.*
import com.ahmetkaragunlu.financeai.viewmodel.AddTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val context = LocalContext.current

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onPhotoSelected(it) }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onCameraPhotoTaken()
        } else {
            viewModel.clearTempCameraPhoto()
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.cameraHelperRef?.onPermissionResult(isGranted)
    }

    // GÜNCELLENDİ: CameraHelper artık onPreparePhoto parametresi alıyor
    val cameraHelper = remember(context, viewModel, cameraLauncher, cameraPermissionLauncher) {
        CameraHelper(
            context = context,
            cameraLauncher = cameraLauncher,
            permissionLauncher = cameraPermissionLauncher,
            onPreparePhoto = viewModel::prepareCameraPhoto // ViewModel'daki fonksiyonu bağlıyoruz
        ).also {
            viewModel.cameraHelperRef = it
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.background))
            .verticalScroll(rememberScrollState())
    ) {
        // ... (Kalan UI kodları tamamen aynı, değişiklik yok)
        // Transaction Type Selection
        Row(
            modifier = modifier
                .padding(16.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { viewModel.updateTransactionType(TransactionType.EXPENSE) },
                modifier = modifier.weight(1f),
                colors = if (viewModel.selectedTransactionType == TransactionType.EXPENSE) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
                } else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(R.string.expense),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = modifier.width(16.dp))
            OutlinedButton(
                onClick = { viewModel.updateTransactionType(TransactionType.INCOME) },
                modifier = Modifier.weight(1f),
                colors = if (viewModel.selectedTransactionType == TransactionType.INCOME) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
                } else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(R.string.income),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = modifier.height(16.dp))

        // Form Fields
        Column(
            modifier = modifier.padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Amount Field
            EditTextField(
                value = viewModel.inputAmount,
                onValueChange = { viewModel.updateInputAmount(it) },
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                placeholder = R.string.enter_amount,
                colors = AddTransactionScreenTextFieldStyles.textFieldColors(),
                trailingIcon = {
                    Text(
                        getCurrencySymbol(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )

            FinanceDropdownMenu(
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth(),
                expanded = viewModel.isCategoryDropdownExpanded,
                onExpandedChange = { isOpen ->
                    if (isOpen) viewModel.toggleDropdown() else viewModel.dismissDropdown()
                },
                options = viewModel.availableCategories,
                onOptionSelected = { category -> viewModel.updateCategory(category) },
                itemLabel = { category -> category.name.replace("_", " ") },
                trigger = {
                    OutlinedTextField(
                        value = viewModel.selectedCategory?.name?.replace("_", " ") ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.select_category),
                                color = Color.Gray
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.clickable { viewModel.toggleDropdown() }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDropdown() },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Color(0xFF404349),
                            disabledTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            )
            // Note Field
            EditTextField(
                value = viewModel.inputNote,
                onValueChange = { viewModel.updateInputNote(it) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                placeholder = R.string.enter_your_note,
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth(),
                colors = AddTransactionScreenTextFieldStyles.textFieldColors()
            )

            // Date Picker
            DatePickerField(
                selectedDate = viewModel.selectedDate,
                onDateClick = { viewModel.openDatePicker() },
                isRemenderEnabled = viewModel.isReminderEnabled,
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth()
            )

            // Reminder Switch
            ReminderSwitch(
                isEnabled = viewModel.isReminderEnabled,
                onToggle = { viewModel.toggleReminder(it) },
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
            )

            // Location & Photo Cards
            Row(
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                // Location Card
                Card(
                    onClick = {
                        viewModel.showLocationPicker = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF404349))
                    ) {
                        if (viewModel.selectedLocation == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.location_optional),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = viewModel.selectedLocation!!.addressShort,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearLocation() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_photo),
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
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
                        if (viewModel.selectedPhotoUri == null) {
                            viewModel.showPhotoBottomSheet = true
                        }
                    },
                    modifier = modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .background(Color(0xFF404349))
                    ) {
                        if (viewModel.selectedPhotoUri == null) {
                            Row(
                                modifier = modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                                Spacer(modifier = modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.photo_optional),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(viewModel.selectedPhotoUri),
                                contentDescription = stringResource(R.string.selected_photo),
                                modifier = modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = { viewModel.clearPhoto() },
                                modifier = modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .padding(2.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.7f),
                                        RoundedCornerShape(50)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove_photo),
                                    tint = Color.White,
                                    modifier = modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            // Save Button
            Button(
                onClick = {
                    viewModel.saveTransaction(
                        onSuccess = {
                            navController.navigateSingleTopClear(Screens.HomeScreen.route)
                            Toast.makeText(
                                context,
                                context.getString(R.string.success),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404349)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(
                        id = if (viewModel.isReminderEnabled)
                            R.string.create_reminder_button else R.string.save_button
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (viewModel.showLocationPicker) {
        MapLocationPickerScreen(
            onLocationSelected = { lat, lon ->
                viewModel.onLocationSelected(lat, lon)
                viewModel.showLocationPicker = false
            },
            onDismiss = {
                viewModel.showLocationPicker = false
            }
        )
    }

    // Photo Source Bottom Sheet
    if (viewModel.showPhotoBottomSheet) {
        PhotoSourceBottomSheet(
            onDismiss = { viewModel.showPhotoBottomSheet = false },
            onCameraClick = { cameraHelper.launchCamera() },
            onGalleryClick = { photoPickerLauncher.launch("image/*") }
        )
    }

    // Date Picker Dialog
    if (viewModel.isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.selectedDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return viewModel.isDateValid(utcTimeMillis)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { viewModel.closeDatePicker() },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF2B2D31)),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            if (viewModel.isDateValid(timestamp)) {
                                viewModel.updateSelectedDate(timestamp)
                                viewModel.closeDatePicker()
                            }
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.ok),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDatePicker() }) {
                    Text(
                        stringResource(id = R.string.cancel),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF2B2D31),
                    dayContentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledDayContentColor = Color.Gray,
                    weekdayContentColor = MaterialTheme.colorScheme.onPrimary,
                    dividerColor = Color(0xFF2B2D31),
                    navigationContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    headlineContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = Color.Gray,
                    todayDateBorderColor = Color.Gray,
                    todayContentColor = Color.Gray
                )
            )
        }
    }
}