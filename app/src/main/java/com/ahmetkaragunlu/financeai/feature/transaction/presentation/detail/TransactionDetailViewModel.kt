package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import com.ahmetkaragunlu.financeai.photo.PhotoWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val photoStore: PhotoLocalStore,
    private val session: AccountSession,
    private val repository: TransactionRepository,
    private val photoWork: PhotoWorkScheduler,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Int = savedStateHandle.get<Int>("transactionId") ?: 0
    val transaction: StateFlow<Transaction?> = repository.observeTransactionById(transactionId)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    var showEditBottomSheet by mutableStateOf(false)
    var showDeleteDialog by mutableStateOf(false)
    var showPhotoZoomDialog by mutableStateOf(false)
    var showPhotoSourceSheet by mutableStateOf(false)

    var editAmount by mutableStateOf("")
    var editNote by mutableStateOf("")
    var editCategory by mutableStateOf<CategoryType?>(null)
    var isCategoryDropdownExpanded by mutableStateOf(false)
    var tempCameraPhotoPath by mutableStateOf<String?>(null)

    val availableCategories: List<CategoryType>
        get() = transaction.value?.let { tx ->
            CategoryType.entries.filter { it.type == tx.transaction }
        } ?: emptyList()

    fun prepareCameraPhoto(): Pair<File, Uri>? {
        val result = PhotoStorageUtil.createTempPhotoFile(context)
        result?.let { (file, _) ->
            tempCameraPhotoPath = file.absolutePath
        }
        return result
    }

    fun onPhotoSelected(uri: Uri) {
        updatePhotoInternal(uri)
    }

    fun onCameraPhotoTaken() {
        tempCameraPhotoPath?.let { path ->
            updatePhotoInternal(Uri.fromFile(File(path)))
        }
    }

    private fun updatePhotoInternal(uri: Uri) {
        val currentTx = transaction.value ?: return
        val account = session.account.value?.takeIf { it.ownerId == currentTx.ownerId } ?: return

        viewModelScope.launch {
            val savedPath = photoStore.save(uri, tempCameraPhotoPath, currentTx.ownerId)
            tempCameraPhotoPath = null
            if (!session.isCurrent(account)) { photoStore.delete(savedPath); return@launch }
            if (savedPath != null) {
                val updatedTx = currentTx.copy(
                    photoUri = savedPath,
                    syncedToFirebase = false
                )
                repository.updateTransaction(updatedTx)
                photoStore.delete(currentTx.photoUri)

                if (updatedTx.firestoreId.isNotEmpty()) {
                    enqueuePhotoUploadWorker(updatedTx)
                }
            }
        }
    }

    private fun enqueuePhotoUploadWorker(transaction: Transaction) {
        photoWork.upload(transaction.ownerId, "transactions", transaction.firestoreId, transaction.photoUri)
    }

    fun deletePhoto() {
        val currentTx = transaction.value ?: return
        viewModelScope.launch {
            val updatedTx = currentTx.copy(photoUri = null, syncedToFirebase = false)
            repository.updateTransaction(updatedTx)
            photoStore.delete(currentTx.photoUri)
            showPhotoZoomDialog = false
        }
    }
    fun openEditBottomSheet() {
        transaction.value?.let { tx ->
            editAmount = tx.amount.toString()
            editNote = tx.note
            editCategory = tx.category
            showEditBottomSheet = true
        }
    }

    fun updateTransaction(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentTransaction = transaction.value ?: return
        val amount = MoneyAmounts.parse(editAmount, currentTransaction.currencyCode)
        if (amount == null || amount <= 0) {
            onError(context.getString(R.string.invalid_amount))
            return
        }
        if (editCategory == null) {
            onError(context.getString(R.string.select_category_error))
            return
        }
        val note = editNote
        val category = editCategory ?: return
        val account = session.account.value?.takeIf { it.ownerId == currentTransaction.ownerId } ?: return
        viewModelScope.launch {
            try {
                if (!session.isCurrent(account)) return@launch
                val updatedTransaction = currentTransaction.copy(
                    amount = amount,
                    note = note,
                    category = category,
                    syncedToFirebase = false
                )
                repository.updateTransaction(updatedTransaction)

                showEditBottomSheet = false
                if (session.isCurrent(account)) onSuccess()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (session.isCurrent(account)) onError(context.getString(R.string.update_failed, e.message ?: ""))
            }
        }
    }

    fun deleteTransaction(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentTransaction = transaction.value ?: return
        val account = session.account.value?.takeIf { it.ownerId == currentTransaction.ownerId } ?: return

        viewModelScope.launch {
            try {
                if (!session.isCurrent(account)) return@launch
                repository.deleteTransaction(currentTransaction)
                photoStore.delete(currentTransaction.photoUri)
                showDeleteDialog = false
                if (session.isCurrent(account)) onSuccess()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (session.isCurrent(account)) onError(context.getString(R.string.delete_failed, e.message ?: ""))
            }
        }
    }
}
