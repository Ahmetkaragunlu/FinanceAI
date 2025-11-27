package com.ahmetkaragunlu.financeai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.photo.PhotoUploadWorker
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val firebaseSyncService: FirebaseSyncService,
    private val workManager: WorkManager,
) : ViewModel() {

    val scheduledTransactions: StateFlow<List<ScheduledTransactionEntity>> =
        repository.getAllScheduledTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun executeScheduledTransaction(scheduledTx: ScheduledTransactionEntity) {
        viewModelScope.launch {
            // 1. YENİ ID OLUŞTURMA (Düzeltme: Artık ID boş kalmayacak)
            val newFirestoreId = firebaseSyncService.getNewTransactionId() //

            // 2. Yeni İşlem Nesnesini Oluştur (Fotoğraf ve Konum verileri kopyalanıyor)
            val newTransaction = TransactionEntity(
                id = 0, // Room otomatik ID verecek
                firestoreId = newFirestoreId, // Firebase ID'sini buraya veriyoruz
                amount = scheduledTx.amount,
                transaction = scheduledTx.type,
                category = scheduledTx.category,
                note = scheduledTx.note ?: "",
                date = System.currentTimeMillis(),
                photoUri = scheduledTx.photoUri, // Yerel dosya yolu kopyalanıyor
                locationFull = scheduledTx.locationFull, // Konum verisi kopyalanıyor
                locationShort = scheduledTx.locationShort,
                latitude = scheduledTx.latitude,
                longitude = scheduledTx.longitude,
                syncedToFirebase = false
            )

            // 3. Yerel Veritabanı İşlemleri
            repository.insertTransaction(newTransaction)
            repository.deleteScheduledTransaction(scheduledTx)
            cancelNotificationWork(scheduledTx.id)

            // 4. Firebase Senkronizasyonu
            syncChanges(newTransaction, scheduledTx)

            // 5. FOTOĞRAF YÜKLEME (Eksik olan kısım: Eğer foto varsa Worker tetiklenmeli)
            if (!newTransaction.photoUri.isNullOrBlank()) {
                enqueuePhotoUploadWorker(newTransaction)
            }
        }
    }

    private fun cancelNotificationWork(scheduledId: Long) {
        workManager.cancelAllWorkByTag("scheduled_notification_$scheduledId")
        workManager.cancelAllWorkByTag("delete_expired_$scheduledId")
    }

    private fun syncChanges(newTx: TransactionEntity, oldScheduledTx: ScheduledTransactionEntity) {
        viewModelScope.launch {
            try {
                // Yeni işlemi Firebase'e gönder (ID artık dolu olduğu için çalışacak)
                if (newTx.firestoreId.isNotEmpty()) {
                    firebaseSyncService.syncTransactionToFirebase(newTx) //
                }

                // Eski planlı işlemi Firebase'den sil
                if (oldScheduledTx.firestoreId.isNotEmpty()) {
                    firebaseSyncService.deleteScheduledTransactionFromFirebase(oldScheduledTx.firestoreId) //
                }
            } catch (e: Exception) {
                // İnternet yoksa hata verir ama sorun değil.
                // Room'da "syncedToFirebase = false" olduğu için
                // SyncService internet gelince bunu otomatik halledecek.
            }
        }
    }

    // Fotoğrafı Firebase Storage'a yüklemek için Worker'ı kuyruğa alan fonksiyon
    private fun enqueuePhotoUploadWorker(transaction: TransactionEntity) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWork = OneTimeWorkRequestBuilder<PhotoUploadWorker>() //
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    PhotoUploadWorker.KEY_LOCAL_PATH to transaction.photoUri,
                    PhotoUploadWorker.KEY_FIRESTORE_ID to transaction.firestoreId,
                    PhotoUploadWorker.KEY_COLLECTION_TYPE to "transactions"
                )
            )
            .build()
        workManager.enqueue(uploadWork)
    }
}