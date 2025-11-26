package com.ahmetkaragunlu.financeai.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val firebaseSyncService: FirebaseSyncService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Int = savedStateHandle.get<Int>("transactionId") ?: 0

    private val _transaction = MutableStateFlow<TransactionEntity?>(null)
    val transaction: StateFlow<TransactionEntity?> = _transaction.asStateFlow()

    var showEditBottomSheet by mutableStateOf(false)
    var showDeleteDialog by mutableStateOf(false)

    // Edit states
    var editAmount by mutableStateOf("")
    var editNote by mutableStateOf("")
    var editCategory by mutableStateOf<CategoryType?>(null)
    var isCategoryDropdownExpanded by mutableStateOf(false)

    val availableCategories: List<CategoryType>
        get() = _transaction.value?.let { tx ->
            CategoryType.entries.filter { it.type == tx.transaction }
        } ?: emptyList()

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            repository.getAllTransactions().collect { transactions ->
                _transaction.value = transactions.find { it.id == transactionId }
                _transaction.value?.let { tx ->
                    editAmount = tx.amount.toString()
                    editNote = tx.note
                    editCategory = tx.category
                }
            }
        }
    }

    fun updateTransaction(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentTransaction = _transaction.value ?: return

        val amount = editAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            onError("Geçersiz tutar")
            return
        }

        if (editCategory == null) {
            onError("Kategori seçiniz")
            return
        }

        viewModelScope.launch {
            try {
                val updatedTransaction = currentTransaction.copy(
                    amount = amount,
                    note = editNote,
                    category = editCategory!!,
                    syncedToFirebase = false
                )

                repository.updateTransaction(updatedTransaction)

                // Firebase sync
                if (updatedTransaction.firestoreId.isNotEmpty()) {
                    launch {
                        try {
                            firebaseSyncService.syncTransactionToFirebase(updatedTransaction)
                            repository.updateTransaction(updatedTransaction.copy(syncedToFirebase = true))
                        } catch (e: Exception) {
                        }
                    }
                }

                showEditBottomSheet = false
                onSuccess()
            } catch (e: Exception) {
                onError("Güncelleme başarısız: ${e.message}")
            }
        }
    }

    fun deleteTransaction(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentTransaction = _transaction.value ?: return

        viewModelScope.launch {
            try {
                if (currentTransaction.firestoreId.isNotEmpty()) {
                    launch {
                        try {
                            firebaseSyncService.deleteTransactionFromFirebase(
                                currentTransaction.firestoreId
                            )
                        } catch (e: Exception) {
                        }
                    }
                }
                currentTransaction.photoUri?.let { photoPath ->
                    PhotoStorageUtil.deletePhoto(photoPath)
                }
                repository.deleteTransaction(currentTransaction)
                showDeleteDialog = false
                onSuccess()
            } catch (e: Exception) {
                onError("Silme başarısız: ${e.message}")
            }
        }
    }

    fun openEditBottomSheet() {
        _transaction.value?.let { tx ->
            editAmount = tx.amount.toString()
            editNote = tx.note
            editCategory = tx.category
            showEditBottomSheet = true
        }
    }
}