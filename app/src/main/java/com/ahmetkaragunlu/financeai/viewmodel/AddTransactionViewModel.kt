package com.ahmetkaragunlu.financeai.viewmodel

import android.content.Context
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
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.worker.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repo: FinanceRepository,
    private val workManager: WorkManager,
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



    fun updateInputNote(note: String) {
        inputNote = note
    }

    fun updateInputAmount(amount: String) {
        inputAmount = amount
    }

    fun updateTransactionType(type: TransactionType) {
        selectedTransactionType = type
        selectedCategory = null
        inputNote = ""
        inputAmount = ""
        selectedDate = System.currentTimeMillis()
        isReminderEnabled = false

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
            try {
                if (isReminderEnabled) {
                    val scheduledTransaction = ScheduledTransactionEntity(
                        amount = amount,
                        type = selectedTransactionType,
                        category = selectedCategory!!,
                        note = inputNote.ifBlank { "" },
                        scheduledDate = selectedDate,
                        notificationSent = false,
                        expirationNotificationSent = false
                    )
                    repo.insertScheduledTransaction(scheduledTransaction)
                    delay(150)

                    val allTransactions = repo.getAllScheduledTransactions().first()

                    val insertedTransaction = allTransactions
                        .filter {
                            it.scheduledDate == selectedDate &&
                                    it.amount == amount &&
                                    !it.notificationSent
                        }
                        .maxByOrNull { it.id }

                    if (insertedTransaction != null) {
                        scheduleFirstNotification(insertedTransaction.id)
                        clearForm()
                        onSuccess()
                    } else {
                        onError(context.getString(R.string.error_reminder_not_created))
                    }

                } else {
                    val transaction = TransactionEntity(
                        amount = amount,
                        transaction = selectedTransactionType,
                        note = inputNote,
                        date = selectedDate,
                        category = selectedCategory!!
                    )
                    repo.insertTransaction(transaction)
                    clearForm()
                    onSuccess()
                }
            } catch (e: Exception) {
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
    }
}