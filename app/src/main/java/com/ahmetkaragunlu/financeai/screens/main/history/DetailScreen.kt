package com.ahmetkaragunlu.financeai.screens.main.history

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.photo.CameraHelper
import com.ahmetkaragunlu.financeai.photo.PhotoSourceBottomSheet
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.ui.theme.AddTransactionScreenTextFieldStyles
import com.ahmetkaragunlu.financeai.utils.*
import com.ahmetkaragunlu.financeai.viewmodel.DetailViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // --- Kamera ve Galeri Başlatıcıları ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onCameraPhotoTaken()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // İzin sonucunu doğrudan helper'a iletemiyoruz çünkü helper remember içinde.
        // Ancak bu yapıda basitçe ekrandan toast veya yeniden başlatma yapabiliriz.
        // Veya AddTransaction'daki gibi viewModel'da tutabiliriz.
        // En temizi burada helper'ı kullanmaktır, helper değişkeni aşağıda tanımlı olduğu için erişemeyiz.
        // Çözüm: remember içine almadan önce tanımlamak veya basit bir check yapmak.
        // AddTransactionScreen ile birebir uyumlu olması için:
        if (isGranted) {
            // İzin verildiyse kamera tekrar başlatılabilir ama kullanıcı butona tekrar basabilir.
            // Kullanıcı deneyimi için bir Toast yeterli.
        } else {
            Toast.makeText(context, "Kamera izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onPhotoSelected(it) }
    }

    // Ortak Helper Kullanımı (Generic)
    val cameraHelper = remember(context, cameraLauncher, permissionLauncher) {
        CameraHelper(
            context = context,
            cameraLauncher = cameraLauncher,
            permissionLauncher = permissionLauncher,
            onPreparePhoto = viewModel::prepareCameraPhoto // ViewModel'daki fonksiyon
        )
    }

    transaction?.let { tx ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = colorResource(R.color.background))
                .verticalScroll(rememberScrollState())
        ) {
            // Main Card
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF404349))
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
                        text = tx.amount.formatAsCurrency(),
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
                    // Note
                    if (tx.note.isNotBlank()) {
                        Text(
                            "Not: ${tx.note}",
                            color = Color.White
                        )
                    }

                    // Location
                    if (tx.locationShort != null) {
                        Text(
                            "Konum: ${tx.locationShort}",
                            color = Color.White
                        )
                    }

                    // Photo Section
                    if (tx.photoUri != null && File(tx.photoUri).exists()) {
                        Spacer(modifier = modifier.height(8.dp))
                        // Tıklanabilir Kart (Büyütme için)
                        Card(
                            modifier = modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clickable { viewModel.showPhotoZoomDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(File(tx.photoUri)),
                                    contentDescription = "İşlem Fotoğrafı",
                                    modifier = modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .padding(4.dp)
                                )
                            }
                        }
                    } else {
                        // Fotoğraf Ekle Butonu
                        OutlinedButton(
                            onClick = { viewModel.showPhotoSourceSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Fotoğraf Ekle")
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.openEditBottomSheet() },
                    modifier = modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404349)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Düzenle",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Button(
                    onClick = { viewModel.showDeleteDialog = true },
                    modifier = modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404349)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Sil",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // --- Photo Zoom Dialog (Tam Ekran) ---
        if (viewModel.showPhotoZoomDialog && tx.photoUri != null) {
            Dialog(
                onDismissRequest = { viewModel.showPhotoZoomDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(File(tx.photoUri)),
                        contentDescription = "Full Screen Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { viewModel.showPhotoZoomDialog = false },
                        contentScale = ContentScale.Fit
                    )

                    IconButton(
                        onClick = { viewModel.showPhotoZoomDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }

                    // Değiştir / Sil Butonları
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.showPhotoSourceSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Değiştir", color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.deletePhoto() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.7f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Sil", color = Color.White)
                        }
                    }
                }
            }
        }

        // --- Photo Source Bottom Sheet ---
        if (viewModel.showPhotoSourceSheet) {
            PhotoSourceBottomSheet(
                onDismiss = { viewModel.showPhotoSourceSheet = false },
                onCameraClick = { cameraHelper.launchCamera() }, // Yeni Helper Kullanımı
                onGalleryClick = { galleryLauncher.launch("image/*") }
            )
        }

        // Edit Bottom Sheet ve Delete Dialog (Aynı kalıyor)
        if (viewModel.showEditBottomSheet) {
            EditBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showEditBottomSheet = false },
                onSave = {
                    viewModel.updateTransaction(
                        onSuccess = { Toast.makeText(context, "Güncellendi", Toast.LENGTH_SHORT).show() },
                        onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                    )
                }
            )
        }

        if (viewModel.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showDeleteDialog = false },
                title = { Text("İşlemi Sil") },
                text = { Text("Bu işlemi silmek istediğinizden emin misiniz?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTransaction(
                            onSuccess = {
                                Toast.makeText(context, "Silindi", Toast.LENGTH_SHORT).show()
                                navController.navigateSingleTopClear(Screens.TRANSACTION_HISTORY_SCREEN.route)
                            },
                            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                        )
                    }) { Text("Sil", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showDeleteDialog = false }) { Text("İptal") }
                }
            )
        }
    }
}

// ... EditBottomSheet aynı kalıyor ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBottomSheet(
    viewModel: DetailViewModel,
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
                text = "İşlemi Düzenle",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )

            // Amount
            EditTextField(
                value = viewModel.editAmount,
                onValueChange = { viewModel.editAmount = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
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

            // Category Dropdown
            FinanceDropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                expanded = viewModel.isCategoryDropdownExpanded,
                onExpandedChange = { viewModel.isCategoryDropdownExpanded = it },
                options = viewModel.availableCategories,
                onOptionSelected = { category ->
                    viewModel.editCategory = category
                    viewModel.isCategoryDropdownExpanded = false
                },
                itemLabel = { category -> stringResource(category.toResId()) },
                trigger = {
                    OutlinedTextField(
                        value = viewModel.editCategory?.let { stringResource(it.toResId()) } ?: "",
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
                                    viewModel.isCategoryDropdownExpanded = true
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.isCategoryDropdownExpanded = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Color(0xFF404349),
                            disabledTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = false,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )

            // Note
            EditTextField(
                value = viewModel.editNote,
                onValueChange = { viewModel.editNote = it },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                placeholder = R.string.enter_your_note,
                modifier = Modifier.fillMaxWidth(),
                colors = AddTransactionScreenTextFieldStyles.textFieldColors()
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
                    "Kaydet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}