package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.location.domain.AddressResolver
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.transactionFailureMessage
import com.ahmetkaragunlu.financeai.photo.PhotoWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.LocalDate
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val photoStore: PhotoLocalStore,
    private val session: AccountSession,
    private val repo: TransactionRepository,
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
    private val photoWork: PhotoWorkScheduler,
    private val addresses: AddressResolver,
    private val clock: Clock,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var isSaving = false
    private var locationJob: Job? = null
    private var locationRequest = 0L

    var selectedTransactionType by mutableStateOf(TransactionType.EXPENSE)
        private set
    var selectedCategory by mutableStateOf<CategoryType?>(null)
        private set

    val availableCategories: List<CategoryType>
        get() = CategoryType.entries.filter { it.type == selectedTransactionType }

    var inputAmount by mutableStateOf("")
        private set
    var inputNote by mutableStateOf("")
        private set
    var selectedDate by mutableLongStateOf(clock.millis())
        private set

    var isReminderEnabled by mutableStateOf(false)
        private set

    var selectedPhotoUri by mutableStateOf<Uri?>(null)
        private set
    var tempCameraPhotoPath by mutableStateOf<String?>(null)
        private set

    var selectedLocation by mutableStateOf<LocationData?>(null)
        private set

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
        selectedDate = clock.millis()
        isReminderEnabled = false
        clearPhoto()
        clearLocation()
    }

    fun updateCategory(category: CategoryType) {
        selectedCategory = category
    }

    fun dateZone(): ZoneId = if (isReminderEnabled) ZoneId.of(session.requireAccount().timeZoneId) else ZoneId.systemDefault()
    fun selectPickerDate(millis: Long) { selectedDate = FinancePeriods.fromPicker(millis, dateZone()) }
    fun pickerDate(): Long = FinancePeriods.toPicker(selectedDate, dateZone())
    fun isPickerDateValid(millis: Long): Boolean = isDateValid(FinancePeriods.fromPicker(millis, dateZone()))

    fun toggleReminder(enabled: Boolean) {
        isReminderEnabled = enabled
        selectedDate = if (!enabled) {
            clock.millis()
        } else {
            LocalDate.now(clock.withZone(dateZone())).plusDays(1).atStartOfDay(dateZone()).toInstant().toEpochMilli()
        }
    }

    fun isDateValid(timestamp: Long): Boolean {
        val today = LocalDate.now(clock.withZone(dateZone())).atStartOfDay(dateZone()).toInstant().toEpochMilli()
        val selected = timestamp

        return if (isReminderEnabled) {
            selected >= today
        } else {
            selected <= clock.millis()
        }
    }

    fun onPhotoSelected(uri: Uri) {
        clearTempCameraPhoto()
        selectedPhotoUri = uri
    }

    fun cameraOwnerId(): String? = session.account.value?.ownerId
    fun registerCameraDraft(path: String) { clearTempCameraPhoto(); tempCameraPhotoPath = path }

    fun onCameraPhotoTaken() {
        tempCameraPhotoPath?.let { path ->
            selectedPhotoUri = Uri.fromFile(File(path))
        }
    }

    fun clearPhoto() {
        selectedPhotoUri = null
        tempCameraPhotoPath?.let { path ->
            viewModelScope.launch { photoStore.delete(path) }
        }
        tempCameraPhotoPath = null
    }

    fun clearTempCameraPhoto() {
        tempCameraPhotoPath?.let { path ->
            viewModelScope.launch { photoStore.delete(path) }
        }
        tempCameraPhotoPath = null
    }

    fun onLocationSelected(latitude: Double, longitude: Double) {
        val account = session.account.value ?: return
        locationJob?.cancel()
        val request = ++locationRequest
        locationJob = viewModelScope.launch {
            try {
                val locationData = addresses.resolve(Coordinates(latitude, longitude))
                if (request == locationRequest && session.isCurrent(account)) {
                    selectedLocation = locationData ?: LocationData(latitude, longitude,
                        context.getString(R.string.location_coordinates, latitude, longitude), context.getString(R.string.location_label))
                    if (locationData == null) Toast.makeText(context, context.getString(R.string.address_not_found), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (request == locationRequest && session.isCurrent(account))
                    Toast.makeText(context, context.getString(R.string.address_not_found), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearLocation() {
        locationRequest++
        locationJob?.cancel()
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
                if (photo != null && savedPhotoPath == null) {
                    if (session.isCurrent(account)) onError(context.getString(R.string.photo_save_failed))
                    return@launch
                }
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
                    scheduledTransactionRepository.insertScheduledTransaction(scheduledTransaction)
                    committed = true
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
                if (session.isCurrent(account)) onError(transactionFailureMessage(context, e, R.string.error_transaction_save_failed))
            } finally { isSaving = false }
        }
    }

    private fun clearForm() {
        inputAmount = ""
        inputNote = ""
        selectedCategory = null
        selectedDate = clock.millis()
        isReminderEnabled = false
        clearPhoto()
        clearLocation()
    }
}
