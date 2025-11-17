package com.ahmetkaragunlu.financeai.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.location.LocationData
import com.ahmetkaragunlu.financeai.location.LocationUtil
import com.ahmetkaragunlu.financeai.notification.NotificationWorker
import com.ahmetkaragunlu.financeai.photo.CameraHelper
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repo: FinanceRepository,
    private val workManager: WorkManager,
    private val firebaseSyncService: FirebaseSyncService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var selectedTransactionType by mutableStateOf(TransactionType.EXPENSE)
    var selectedCategory by mutableStateOf<CategoryType?>(null)
    var isCategoryDropdownExpanded by mutableStateOf(false)

    val availableCategories: List<CategoryType>
        get() = CategoryType.entries.filter { it.type == selectedTransactionType }

    var inputAmount by mutableStateOf("")
        private set
    var inputNote by mutableStateOf("")
        private set
    var selectedDate by mutableLongStateOf(System.currentTimeMillis())

    var isReminderEnabled by mutableStateOf(false)
    var isDatePickerOpen by mutableStateOf(false)

    var selectedPhotoUri by mutableStateOf<Uri?>(null)
    var tempCameraPhotoPath by mutableStateOf<String?>(null)
    var showPhotoBottomSheet by mutableStateOf(false)

    var selectedLocation by mutableStateOf<LocationData?>(null)
    var showLocationPicker by mutableStateOf(false)
    var cameraHelperRef by mutableStateOf<CameraHelper?>(null)

    fun updateInputNote(note: String) {
        inputNote = note
    }

    fun updateInputAmount(amount: String) {
        inputAmount = amount
    }

    fun updateTransactionType(type: TransactionType) {
        if (selectedTransactionType == type) return
        selectedTransactionType = type
        selectedCategory = null
        inputNote = ""
        inputAmount = ""
        selectedDate = System.currentTimeMillis()
        isReminderEnabled = false
        clearPhoto()
        clearLocation()
    }

    fun updateCategory(category: CategoryType) {
        selectedCategory = category
    }

    fun toggleDropdown() {
        isCategoryDropdownExpanded = !isCategoryDropdownExpanded
    }

    fun dismissDropdown() {
        isCategoryDropdownExpanded = false
    }

    fun updateSelectedDate(date: Long) {
        selectedDate = date
    }

    fun toggleReminder(enabled: Boolean) {
        isReminderEnabled = enabled
        selectedDate = if (!enabled) {
            System.currentTimeMillis()
        } else {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
    fun openDatePicker() {
        isDatePickerOpen = true
    }

    fun closeDatePicker() {
        isDatePickerOpen = false
    }

    fun isDateValid(timestamp: Long): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return if (isReminderEnabled) {
            timestamp >= today
        } else {
            timestamp <= System.currentTimeMillis()
        }
    }

    fun onPhotoSelected(uri: Uri) {
        selectedPhotoUri = uri
    }

    fun prepareCameraPhoto(): Pair<File, Uri>? {
        val result = PhotoStorageUtil.createTempPhotoFile(context)
        result?.let { (file, _) ->
            tempCameraPhotoPath = file.absolutePath
        }
        return result
    }

    fun onCameraPhotoTaken() {
        tempCameraPhotoPath?.let { path ->
            selectedPhotoUri = Uri.fromFile(File(path))
        }
    }

    fun clearPhoto() {
        selectedPhotoUri = null
        tempCameraPhotoPath?.let { path ->
            PhotoStorageUtil.deletePhoto(path)
        }
        tempCameraPhotoPath = null
    }

    fun clearTempCameraPhoto() {
        tempCameraPhotoPath?.let { path ->
            PhotoStorageUtil.deletePhoto(path)
        }
        tempCameraPhotoPath = null
    }
    @SuppressLint("StringFormatInvalid")
    fun onLocationSelected(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val locationData = LocationUtil.getAddressFromLocation(
                    context,
                    latitude,
                    longitude
                )
                selectedLocation = locationData
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.failure, e.message ?: "")
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun clearLocation() {
        selectedLocation = null
    }
    fun saveTransaction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (inputAmount.isBlank() || inputAmount.toDoubleOrNull() == null) {
            onError(context.getString(R.string.error_invalid_amount))
            return
        }
        if (selectedCategory == null) {
            onError(context.getString(R.string.error_select_category))
            return
        }
        val amount = inputAmount.toDouble()
        viewModelScope.launch {
            var savedPhotoPath: String? = null
            try {
                savedPhotoPath = selectedPhotoUri?.let { uri ->
                    if (tempCameraPhotoPath != null) {
                        PhotoStorageUtil.saveTempPhotoAsPermanent(context, tempCameraPhotoPath!!)
                    } else {
                        PhotoStorageUtil.savePhotoToInternalStorage(context, uri)
                    }
                }

                if (isReminderEnabled) {
                    val scheduledTransaction = ScheduledTransactionEntity(
                        amount = amount,
                        type = selectedTransactionType,
                        category = selectedCategory!!,
                        note = inputNote.ifBlank { "" },
                        scheduledDate = selectedDate,
                        notificationSent = false,
                        expirationNotificationSent = false,
                        photoUri = savedPhotoPath,
                        locationFull = selectedLocation?.addressFull,
                        locationShort = selectedLocation?.addressShort,
                        latitude = selectedLocation?.latitude,
                        longitude = selectedLocation?.longitude,
                        syncedToFirebase = false
                    )
                    val syncResult = firebaseSyncService.syncScheduledTransactionToFirebase(scheduledTransaction)
                    if (syncResult.isSuccess) {
                        val firestoreId = syncResult.getOrNull()!!
                        val transactionWithFirestoreId = scheduledTransaction.copy(
                            firestoreId = firestoreId,
                            syncedToFirebase = true
                        )
                        repo.insertScheduledTransaction(transactionWithFirestoreId)
                        clearForm()
                        onSuccess()
                    } else {
                        val localId = repo.insertScheduledTransaction(scheduledTransaction)
                        scheduleFirstNotificationOffline(localId)
                        clearForm()
                        onSuccess()
                    }

                } else {
                    val transaction = TransactionEntity(
                        amount = amount,
                        transaction = selectedTransactionType,
                        note = inputNote,
                        date = selectedDate,
                        category = selectedCategory!!,
                        photoUri = savedPhotoPath,
                        locationFull = selectedLocation?.addressFull,
                        locationShort = selectedLocation?.addressShort,
                        latitude = selectedLocation?.latitude,
                        longitude = selectedLocation?.longitude,
                        syncedToFirebase = false
                    )
                    val syncResult = firebaseSyncService.syncTransactionToFirebase(transaction)
                    if (syncResult.isSuccess) {
                        val firestoreId = syncResult.getOrNull()!!
                        val transactionWithFirestoreId = transaction.copy(
                            firestoreId = firestoreId,
                            syncedToFirebase = true
                        )
                        repo.insertTransaction(transactionWithFirestoreId)
                        clearForm()
                        onSuccess()
                    } else {
                        repo.insertTransaction(transaction)
                        clearForm()
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                savedPhotoPath?.let { PhotoStorageUtil.deletePhoto(it) }
                onError(context.getString(R.string.error_transaction_save_failed, e.message ?: ""))
            }
        }
    }

    private fun scheduleFirstNotificationOffline(transactionId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    NotificationWorker.TRANSACTION_ID_KEY to transactionId
                )
            )
            .addTag("scheduled_notification_$transactionId")
            .build()
        workManager.enqueue(workRequest)
    }
    private fun clearForm() {
        inputAmount = ""
        inputNote = ""
        selectedCategory = null
        selectedDate = System.currentTimeMillis()
        isReminderEnabled = false
        clearPhoto()
        clearLocation()
    }
}