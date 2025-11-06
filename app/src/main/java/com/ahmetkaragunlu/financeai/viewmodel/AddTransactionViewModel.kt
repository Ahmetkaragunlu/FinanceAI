

// ============================================
// AddTransactionViewModel.kt - GERÇEK ÇÖZÜM
// ============================================
package com.ahmetkaragunlu.financeai.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
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
import com.ahmetkaragunlu.financeai.firebaserepo.FirebaseSyncService
import com.ahmetkaragunlu.financeai.location.LocationData
import com.ahmetkaragunlu.financeai.location.LocationUtil
import com.ahmetkaragunlu.financeai.photo.CameraHelper
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.worker.NotificationWorker
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

    companion object {
        private const val TAG = "AddTransactionVM"
    }

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
        if (!enabled) {
            selectedDate = System.currentTimeMillis()
        } else {
            selectedDate = Calendar.getInstance().apply {
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
        result?.let { (file, uri) ->
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
                Log.e(TAG, "Error getting location", e)
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
            var savedPhotoPath: String? = null // Try bloğunun DIŞINDA tanımla
            try {
                savedPhotoPath = selectedPhotoUri?.let { uri ->
                    if (tempCameraPhotoPath != null) {
                        PhotoStorageUtil.saveTempPhotoAsPermanent(context, tempCameraPhotoPath!!)
                    } else {
                        PhotoStorageUtil.savePhotoToInternalStorage(context, uri)
                    }
                }

                if (isReminderEnabled) {
                    // ============================================
                    // SCHEDULED TRANSACTION (Hatırlatıcılı)
                    // ============================================
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
                        syncedToFirebase = false // HENÜZ SYNC EDİLMEDİ
                    )

                    // 1. Önce Firebase'e kaydet ve firestoreId al
                    val syncResult = firebaseSyncService.syncScheduledTransactionToFirebase(scheduledTransaction)

                    if (syncResult.isSuccess) {
                        val firestoreId = syncResult.getOrNull()!!
                        Log.d(TAG, "Scheduled transaction synced to Firebase: $firestoreId")

                        // 2. Sonra firestoreId ile birlikte local'e kaydet (TEK SEFER!)
                        val transactionWithFirestoreId = scheduledTransaction.copy(
                            firestoreId = firestoreId,
                            syncedToFirebase = true
                        )
                        val localId = repo.insertScheduledTransaction(transactionWithFirestoreId)

                        // 3. Bildirimi planla
                        scheduleFirstNotification(localId)
                        clearForm()
                        onSuccess()
                    } else {
                        // Firebase başarısız, sadece local'e kaydet
                        Log.e(TAG, "Failed to sync to Firebase", syncResult.exceptionOrNull())
                        val localId = repo.insertScheduledTransaction(scheduledTransaction)
                        scheduleFirstNotification(localId)
                        clearForm()
                        onSuccess()
                    }

                } else {
                    // ============================================
                    // NORMAL TRANSACTION (Hatırlatıcısız)
                    // ============================================
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
                        syncedToFirebase = false // HENÜZ SYNC EDİLMEDİ
                    )

                    // 1. Önce Firebase'e kaydet ve firestoreId al
                    val syncResult = firebaseSyncService.syncTransactionToFirebase(transaction)

                    if (syncResult.isSuccess) {
                        val firestoreId = syncResult.getOrNull()!!
                        Log.d(TAG, "Transaction synced to Firebase: $firestoreId")

                        // 2. Sonra firestoreId ile birlikte local'e kaydet (TEK SEFER!)
                        val transactionWithFirestoreId = transaction.copy(
                            firestoreId = firestoreId,
                            syncedToFirebase = true
                        )
                        repo.insertTransaction(transactionWithFirestoreId)
                        clearForm()
                        onSuccess()
                    } else {
                        // Firebase başarısız, sadece local'e kaydet
                        Log.e(TAG, "Failed to sync transaction to Firebase", syncResult.exceptionOrNull())
                        repo.insertTransaction(transaction)
                        clearForm()
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transaction", e)
                savedPhotoPath?.let { PhotoStorageUtil.deletePhoto(it) }
                onError(context.getString(R.string.error_transaction_save_failed, e.message ?: ""))
            }
        }
    }

    private fun scheduleFirstNotification(transactionId: Long) {
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
