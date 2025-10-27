package com.ahmetkaragunlu.financeai.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repo: FinanceRepository
) : ViewModel() {

    // YENİ: Tüm scheduled işlemleri listele
    // GÖREVI: ScheduledTransaction listesini sürekli dinler
    val scheduledTransactions: StateFlow<List<ScheduledTransactionEntity>> =
        repo.getAllScheduledTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // YENİ: Scheduled işlemi ONAYLAMA (Bildirimi onaylayınca veya erkenden "ÖDENDİ" yapınca)
    // MANTIK:
    // 1. ScheduledTransaction'dan sil
    // 2. TransactionEntity'ye ekle (bugünün tarihi ile)
    // 3. Artık toplam gelir/gider ETKİLENİR
    fun confirmScheduledTransaction(
        scheduledTransaction: ScheduledTransactionEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // 1. Normal transaction olarak kaydet
                val transaction = TransactionEntity(
                    amount = scheduledTransaction.amount,
                    transaction = scheduledTransaction.type,
                    note = scheduledTransaction.note ?: "",
                    date = System.currentTimeMillis(), // Onaylandığı anın tarihi
                    category = scheduledTransaction.category
                )
                repo.insertTransaction(transaction)

                // 2. Scheduled transaction'ı sil
                repo.deleteScheduledTransaction(scheduledTransaction)

                onSuccess()
            } catch (e: Exception) {
                onError("İşlem onaylanamadı: ${e.message}")
            }
        }
    }

    // YENİ: Scheduled işlemi SİLME (İptal etme)
    // MANTIK: Sadece ScheduledTransaction'dan sil, TransactionEntity'ye EKLEME
    fun deleteScheduledTransaction(
        scheduledTransaction: ScheduledTransactionEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.deleteScheduledTransaction(scheduledTransaction)
                onSuccess()
            } catch (e: Exception) {
                onError("İşlem silinemedi: ${e.message}")
            }
        }
    }
}
