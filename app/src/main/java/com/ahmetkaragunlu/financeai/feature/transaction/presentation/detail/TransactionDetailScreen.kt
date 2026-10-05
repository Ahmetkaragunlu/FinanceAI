package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.app.navigation.Screens
import com.ahmetkaragunlu.financeai.app.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.core.format.formatRelativeDate
import com.ahmetkaragunlu.financeai.core.ui.component.EditAlertDialog
import com.ahmetkaragunlu.financeai.core.ui.component.EditTextField
import com.ahmetkaragunlu.financeai.core.ui.component.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsAccountCurrency
import com.ahmetkaragunlu.financeai.core.ui.component.getAccountCurrencySymbol
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toIconResId
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toResId
import com.ahmetkaragunlu.financeai.photo.CameraHelper
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import com.ahmetkaragunlu.financeai.photo.PhotoSourceBottomSheet
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEditBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoZoomDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoSourceSheet by rememberSaveable { mutableStateOf(false) }
    var isCategoryDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var cameraHelperRef by remember { mutableStateOf<CameraHelper?>(null) }
    LaunchedEffect(viewModel.photoErrorResId) {
        viewModel.photoErrorResId?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show()
            viewModel.consumePhotoError()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onCameraPhotoTaken()
        } else viewModel.clearCameraDraft()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraHelperRef?.onPermissionResult(isGranted)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onPhotoSelected(it) }
    }

    val cameraHelper = remember(context, cameraLauncher, permissionLauncher) {
        CameraHelper(
            context = context,
            cameraLauncher = cameraLauncher,
            permissionLauncher = permissionLauncher,
            onPreparePhoto = {
                viewModel.cameraOwnerId()?.let { owner ->
                    PhotoStorageUtil.createTempPhotoFile(context, owner)?.also { (file, _) ->
                        viewModel.registerCameraDraft(file.absolutePath)
                    }
                }
            }
        ).also { cameraHelperRef = it }
    }

    transaction?.let { tx ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = colorResource(R.color.background))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Card with Gradient
            Card(
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3b4351),
                                    Color(0xFF2d3139)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                painter = painterResource(tx.category.toIconResId()),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }
                        Spacer(modifier = modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(tx.category.toResId()),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = tx.date.formatRelativeDate(context),
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(modifier = modifier.weight(1f))

                        Text(
                            text = tx.amount.formatAsAccountCurrency(),
                            color = if (tx.transaction == TransactionType.INCOME) Color.Green else Color.Red
                        )
                    }

                    // Optional Info Section
                    Column(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (tx.note.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.note_with_value, tx.note),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        if (tx.locationShort != null) {
                            Text(
                                text = stringResource(R.string.location_with_value, tx.locationShort),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Photo Section
                        if (tx.photoUri != null && (tx.photoUri.startsWith("https://") || File(tx.photoUri).exists())) {
                            Spacer(modifier = modifier.height(8.dp))
                            Card(
                                modifier = modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clickable { showPhotoZoomDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(tx.photoUri),
                                        contentDescription = stringResource(R.string.transaction_photo_desc),
                                        modifier = modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .padding(4.dp)
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showPhotoSourceSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.add_photo))
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (viewModel.prepareEdit()) showEditBottomSheet = true },
                    modifier = modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor =Color(0xFF353b45)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edit),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor =Color(0xFF353b45)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // --- Photo Zoom Dialog ---
        if (showPhotoZoomDialog && tx.photoUri != null) {
            Dialog(
                onDismissRequest = { showPhotoZoomDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(tx.photoUri),
                        contentDescription = stringResource(R.string.full_screen_photo_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showPhotoZoomDialog = false },
                        contentScale = ContentScale.Fit
                    )

                    IconButton(
                        onClick = { showPhotoZoomDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { showPhotoSourceSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.change))
                        }
                        Button(
                            onClick = { viewModel.deletePhoto(onSuccess = { showPhotoZoomDialog = false }) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
        // --- Photo Source Bottom Sheet ---
        if (showPhotoSourceSheet) {
            PhotoSourceBottomSheet(
                onDismiss = { showPhotoSourceSheet = false },
                onCameraClick = { cameraHelper.launchCamera() },
                onGalleryClick = { galleryLauncher.launch("image/*") }
            )
        }
        // Edit Bottom Sheet
        // A restored UI flag must not reopen a form whose ViewModel draft was lost with the process.
        if (showEditBottomSheet && viewModel.editCategory != null) {
            EditBottomSheet(
                amount = viewModel.editAmount,
                note = viewModel.editNote,
                category = viewModel.editCategory,
                categories = viewModel.availableCategories,
                onAmountChange = viewModel::updateEditAmount,
                onNoteChange = viewModel::updateEditNote,
                onCategoryChange = viewModel::updateEditCategory,
                categoryDropdownExpanded = isCategoryDropdownExpanded,
                onCategoryDropdownExpandedChange = { isCategoryDropdownExpanded = it },
                onDismiss = { showEditBottomSheet = false },
                onSave = {
                    viewModel.updateTransaction(
                        onSuccess = {
                            showEditBottomSheet = false
                            Toast.makeText(context, context.getString(R.string.updated_successfully), Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }
        if (showDeleteDialog) {
            EditAlertDialog(
                title = R.string.delete_transaction_title,
                text = R.string.delete_transaction_message,
                onDismissRequest = { showDeleteDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTransaction(
                            onSuccess = {
                                showDeleteDialog = false
                                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show()
                                navController.navigateSingleTopClear(Screens.TRANSACTION_HISTORY_SCREEN.route)
                            },
                            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                        )
                    }) { Text(stringResource(R.string.delete), color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBottomSheet(
    amount: String,
    note: String,
    category: CategoryType?,
    categories: List<CategoryType>,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCategoryChange: (CategoryType) -> Unit,
    categoryDropdownExpanded: Boolean,
    onCategoryDropdownExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2D31)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_transaction_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            // Amount
            EditTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                placeholder = R.string.enter_amount,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor =Color(0xFF404349),
                    focusedContainerColor =Color(0xFF404349),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                ),
                trailingIcon = {
                    Text(
                        getAccountCurrencySymbol(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )
            // Category Dropdown
            FinanceDropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                expanded = categoryDropdownExpanded,
                onExpandedChange = onCategoryDropdownExpandedChange,
                options = categories,
                onOptionSelected = { category ->
                    onCategoryChange(category)
                    onCategoryDropdownExpandedChange(false)
                },
                itemLabel = { category -> stringResource(category.toResId()) },
                trigger = {
                    OutlinedTextField(
                        value = category?.let { stringResource(it.toResId()) } ?: "",
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
                                modifier = Modifier.clickable {
                                    onCategoryDropdownExpandedChange(true)
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryDropdownExpandedChange(true) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Color(0xFF404349),
                            disabledTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = false,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
            //Note
            EditTextField(
                value = note,
                onValueChange = onNoteChange,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                placeholder = R.string.enter_your_note,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor =Color(0xFF404349),
                    focusedContainerColor =Color(0xFF404349),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                )
            )

            // Save Button
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404349)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
