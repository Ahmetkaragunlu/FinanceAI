package com.ahmetkaragunlu.financeai.viewmodel

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
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import com.ahmetkaragunlu.financeai.photo.PhotoUploadWorker
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val firebaseSyncService: FirebaseSyncService,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Int = savedStateHandle.get<Int>("transactionId") ?: 0
    val transaction: StateFlow<TransactionEntity?> = repository.getTransactionById(transactionId)
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

        viewModelScope.launch {
            currentTx.photoUri?.let { oldPath ->
                PhotoStorageUtil.deletePhoto(oldPath)
            }
            val savedPath = if (tempCameraPhotoPath != null && uri.path?.contains(tempCameraPhotoPath!!) == true) {
                PhotoStorageUtil.saveTempPhotoAsPermanent(context, tempCameraPhotoPath!!)
            } else {
                PhotoStorageUtil.savePhotoToInternalStorage(context, uri)
            }
            tempCameraPhotoPath = null
            if (savedPath != null) {
                val updatedTx = currentTx.copy(
                    photoUri = savedPath,
                    syncedToFirebase = false
                )
                repository.updateTransaction(updatedTx)

                if (updatedTx.firestoreId.isNotEmpty()) {
                    enqueuePhotoUploadWorker(updatedTx)
                }
            }
        }
    }

    private fun enqueuePhotoUploadWorker(transaction: TransactionEntity) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWork = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
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

    fun deletePhoto() {
        val currentTx = transaction.value ?: return
        viewModelScope.launch {
            currentTx.photoUri?.let { path ->
                PhotoStorageUtil.deletePhoto(path)
            }
            val updatedTx = currentTx.copy(photoUri = null, syncedToFirebase = false)
            repository.updateTransaction(updatedTx)
            showPhotoZoomDialog = false
            if (updatedTx.firestoreId.isNotEmpty()) {
                launch {
                    firebaseSyncService.deleteTransactionPhoto(updatedTx.firestoreId)
                }
            }
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
        val amount = editAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            onError(context.getString(R.string.invalid_amount))
            return
        }
        if (editCategory == null) {
            onError(context.getString(R.string.select_category_error))
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
                onError(context.getString(R.string.update_failed, e.message ?: ""))
            }
        }
    }

    fun deleteTransaction(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentTransaction = transaction.value ?: return

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
                onError(context.getString(R.string.delete_failed, e.message ?: ""))
            }
        }
    }
}