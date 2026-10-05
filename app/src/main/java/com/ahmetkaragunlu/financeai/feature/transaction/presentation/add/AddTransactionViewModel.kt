package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.location.data.LocationUtil
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import com.ahmetkaragunlu.financeai.notification.NotificationWorker
import com.ahmetkaragunlu.financeai.photo.CameraHelper
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import com.ahmetkaragunlu.financeai.photo.PhotoWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val photoStore: PhotoLocalStore,
    private val session: AccountSession,
    private val repo: TransactionRepository,
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
    private val workManager: WorkManager,
    private val photoWork: PhotoWorkScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var isSaving = false

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
                if (e is CancellationException) throw e
                val errorMessage = context.getString(R.string.failure, e.message ?: "")
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearLocation() {
        selectedLocation = null
    }

    fun saveTransaction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isSaving) return
        val account = session.account.value ?: return
        if (MoneyAmounts.parse(inputAmount, account.currencyCode) == null) {
            onError(context.getString(R.string.error_invalid_amount))
            return
        }
        if (selectedCategory == null) {
            onError(context.getString(R.string.error_select_category))
            return
        }
        val amount = MoneyAmounts.parse(inputAmount, account.currencyCode) ?: return

        val category = selectedCategory ?: return
        val type = selectedTransactionType
        val note = inputNote
        val date = selectedDate
        val location = selectedLocation
        val photo = selectedPhotoUri
        val cameraPath = tempCameraPhotoPath
        val isScheduled = isReminderEnabled
        isSaving = true

        viewModelScope.launch {
            var savedPhotoPath: String? = null
            var committed = false
            try {
                savedPhotoPath = photo?.let { photoStore.save(it, cameraPath, account.ownerId) }
                val firestoreId = UUID.randomUUID().toString()
                if (isScheduled) {
                    val scheduledTransaction = ScheduledTransaction(
                        ownerId = account.ownerId, currencyCode = account.currencyCode,
                        amount = amount,
                        type = type,
                        category = category,
                        note = note,
                        scheduledDate = date,
                        notificationSent = false,
                        expirationNotificationSent = false,
                        photoUri = savedPhotoPath,
                        locationFull = location?.addressFull,
                        locationShort = location?.addressShort,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        syncedToFirebase = false,
                        firestoreId = firestoreId
                    )
                    val localId = scheduledTransactionRepository.insertScheduledTransaction(scheduledTransaction)
                    committed = true
                    if (session.isCurrent(account)) scheduleFirstNotificationOffline(localId, account.ownerId)
                } else {
                    val transaction = Transaction(
                        ownerId = account.ownerId, currencyCode = account.currencyCode,
                        amount = amount,
                        transaction = type,
                        note = note,
                        date = date,
                        category = category,
                        photoUri = savedPhotoPath,
                        locationFull = location?.addressFull,
                        locationShort = location?.addressShort,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        syncedToFirebase = false,
                        firestoreId = firestoreId
                    )
                    repo.insertTransaction(transaction)
                    committed = true
                }

                if (session.isCurrent(account)) photoWork.upload(account.ownerId,
                    if (isScheduled) "scheduled" else "transactions", firestoreId, savedPhotoPath)

                if (session.isCurrent(account)) { clearForm(); onSuccess() }
            } catch (e: Exception) {

                if (!committed) withContext(NonCancellable) { photoStore.delete(savedPhotoPath) }
                if (e is CancellationException) throw e
                if (session.isCurrent(account)) onError(context.getString(R.string.error_transaction_save_failed, e.message ?: ""))
            } finally { isSaving = false }
        }
    }

    private fun scheduleFirstNotificationOffline(transactionId: Long, ownerId: String) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    NotificationWorker.TRANSACTION_ID_KEY to transactionId,
                    SyncScheduler.OWNER_ID to ownerId
                )
            )
            .addTag("scheduled_notification_$transactionId")
            .addTag("account_${ownerId}")
            .build()
        workManager.enqueueUniqueWork(
            "scheduled_notification_$transactionId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
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
