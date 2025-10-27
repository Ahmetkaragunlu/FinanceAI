
// ============================================
// 3. AddTransactionViewModel.kt
// ============================================
package com.ahmetkaragunlu.financeai.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.worker.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repo: FinanceRepository,
    private val workManager: WorkManager
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
    var selectedDate by mutableStateOf(System.currentTimeMillis())
        private set
    var isReminderEnabled by mutableStateOf(false)
        private set
    var isDatePickerOpen by mutableStateOf(false)
        private set

    val minSelectableDate: Long
        get() {
            if (isReminderEnabled) {
                return Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else {
                return 0L
            }
        }

    val maxSelectableDate: Long
        get() {
            if (isReminderEnabled) {
                return Long.MAX_VALUE
            } else {
                return System.currentTimeMillis()
            }
        }

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
            timestamp > today
        } else {
            timestamp <= System.currentTimeMillis()
        }
    }

    fun saveTransaction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (inputAmount.isBlank() || inputAmount.toDoubleOrNull() == null) {
            onError("Lütfen geçerli bir tutar girin")
            return
        }
        if (selectedCategory == null) {
            onError("Lütfen bir kategori seçin")
            return
        }

        val amount = inputAmount.toDouble()

        viewModelScope.launch {
            try {
                if (isReminderEnabled) {
                    Log.d(TAG, "🔔 Hatırlatıcı modu aktif")

                    // TEST: 5 saniye sonra bildirim (gerçek kullanımda selectedDate kullanılacak)
                    val testScheduledTime = System.currentTimeMillis() + 5000
                    Log.d(TAG, "⏰ Planlanan zaman: $testScheduledTime (5 saniye sonra)")

                    val scheduledTransaction = ScheduledTransactionEntity(
                        amount = amount,
                        type = selectedTransactionType,
                        category = selectedCategory!!,
                        note = inputNote.ifBlank { null },
                        scheduledDate = testScheduledTime,
                        isConfirmed = false,
                        notificationSent = false,
                        expirationNotificationSent = false // YENİ ALAN
                    )

                    // Transaction'ı kaydet
                    Log.d(TAG, "💾 Transaction kaydediliyor...")
                    repo.insertScheduledTransaction(scheduledTransaction)
                    Log.d(TAG, "✅ Transaction kaydedildi")

                    // Kısa bekleyip ID'yi bul
                    Log.d(TAG, "⏳ 150ms bekleniyor...")
                    delay(150)

                    val allTransactions = repo.getAllScheduledTransactions().first()
                    Log.d(TAG, "📊 Toplam scheduled transaction: ${allTransactions.size}")

                    val insertedTransaction = allTransactions
                        .filter {
                            it.scheduledDate == testScheduledTime &&
                                    it.amount == amount &&
                                    !it.notificationSent
                        }
                        .maxByOrNull { it.id }

                    Log.d(TAG, "🔍 Bulunan transaction ID: ${insertedTransaction?.id}")

                    if (insertedTransaction != null) {
                        Log.d(TAG, "🚀 WorkManager ile bildirim planlanıyor - ID: ${insertedTransaction.id}")
                        scheduleNotification(insertedTransaction.id, 5)
                        Log.d(TAG, "✅ WorkManager request kuyruğa eklendi")
                        clearForm()
                        onSuccess()
                    } else {
                        Log.e(TAG, "❌ Transaction bulunamadı!")
                        Log.e(TAG, "Aranan: time=$testScheduledTime, amount=$amount")
                        allTransactions.forEach {
                            Log.e(TAG, "Mevcut: id=${it.id}, time=${it.scheduledDate}, amount=${it.amount}, sent=${it.notificationSent}")
                        }
                        onError("Hatırlatıcı oluşturulamadı")
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
                onError("İşlem kaydedilemedi: ${e.message}")
            }
        }
    }

    private fun scheduleNotification(transactionId: Long, delaySeconds: Long) {
        Log.d(TAG, "📱 scheduleNotification çağrıldı - ID: $transactionId, Delay: $delaySeconds saniye")

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    NotificationWorker.TRANSACTION_ID_KEY to transactionId
                )
            )
            .addTag("scheduled_notification_$transactionId")
            .build()

        workManager.enqueue(workRequest)
        Log.d(TAG, "✅ WorkRequest ID: ${workRequest.id}")
    }

    private fun clearForm() {
        inputAmount = ""
        inputNote = ""
        selectedCategory = null
        selectedDate = System.currentTimeMillis()
        isReminderEnabled = false
    }
}