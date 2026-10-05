package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.transactionFailureMessage
import com.ahmetkaragunlu.financeai.photo.PhotoWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    var editAmount by mutableStateOf("")
        private set
    var editNote by mutableStateOf("")
        private set
    var editCategory by mutableStateOf<CategoryType?>(null)
        private set
    var tempCameraPhotoPath by mutableStateOf<String?>(null)
        private set
    var photoErrorResId by mutableStateOf<Int?>(null)
        private set
    private val photoUpdates = Mutex()
    private var photoRequest = 0L
    fun consumePhotoError() { photoErrorResId = null }

    val availableCategories: List<CategoryType>
        get() = transaction.value?.let { tx ->
            CategoryType.entries.filter { it.type == tx.transaction }
        } ?: emptyList()

    fun cameraOwnerId(): String? = session.account.value?.ownerId
    fun registerCameraDraft(path: String) { clearCameraDraft(); tempCameraPhotoPath = path }
    fun clearCameraDraft() {
        val path = tempCameraPhotoPath
        tempCameraPhotoPath = null
        viewModelScope.launch { photoStore.delete(path) }
    }

    fun onPhotoSelected(uri: Uri) { clearCameraDraft(); updatePhotoInternal(uri, null) }

    fun onCameraPhotoTaken() {
        tempCameraPhotoPath?.let { path ->
            updatePhotoInternal(Uri.fromFile(File(path)), path)
        }
    }

    private fun updatePhotoInternal(uri: Uri, cameraPath: String?) {
        val currentTx = transaction.value ?: return
        val account = session.account.value?.takeIf { it.ownerId == currentTx.ownerId } ?: return

        val request = ++photoRequest
        photoErrorResId = null
        viewModelScope.launch {
            photoUpdates.withLock {
                var saved: String? = null
                var committed = false
                try {
                    if (request != photoRequest || !session.isCurrent(account)) return@withLock
                    saved = photoStore.save(uri, cameraPath, account.ownerId)
                    if (tempCameraPhotoPath == cameraPath) tempCameraPhotoPath = null
                    if (saved == null) { photoErrorResId = R.string.photo_save_failed; return@withLock }
                    if (request != photoRequest || !session.isCurrent(account)) return@withLock
                    val latest = repository.observeTransactionById(transactionId).first() ?: return@withLock
                    val updated = latest.copy(photoUri = saved, syncedToFirebase = false)
                    repository.updateTransaction(updated)
                    committed = true
                    photoStore.delete(latest.photoUri)
                    photoWork.upload(updated.ownerId, "transactions", updated.firestoreId, updated.photoUri)
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { if (session.isCurrent(account)) photoErrorResId = R.string.photo_save_failed }
                finally {
                    if (!committed) withContext(NonCancellable) { photoStore.delete(saved) }
                }
            }
        }
    }

    fun deletePhoto(onSuccess: () -> Unit) {
        val currentTx = transaction.value ?: return
        val account = session.account.value?.takeIf { it.ownerId == currentTx.ownerId } ?: return
        photoRequest++
        photoErrorResId = null
        viewModelScope.launch {
            photoUpdates.withLock {
                try {
                    if (!session.isCurrent(account)) return@withLock
                    val latest = repository.observeTransactionById(transactionId).first() ?: return@withLock
                    repository.updateTransaction(latest.copy(photoUri = null, syncedToFirebase = false))
                    photoStore.delete(latest.photoUri)
                    if (session.isCurrent(account)) onSuccess()
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { if (session.isCurrent(account)) photoErrorResId = R.string.photo_delete_failed }
            }
        }
    }
    fun prepareEdit(): Boolean {
        val tx = transaction.value ?: return false
        editAmount = tx.amount.toString()
        editNote = tx.note
        editCategory = tx.category
        return true
    }

    fun updateEditAmount(amount: String) { editAmount = amount }
    fun updateEditNote(note: String) { editNote = note }
    fun updateEditCategory(category: CategoryType) { editCategory = category }

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

                if (session.isCurrent(account)) onSuccess()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (session.isCurrent(account)) onError(transactionFailureMessage(context, e, R.string.update_failed))
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
                if (session.isCurrent(account)) onSuccess()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (session.isCurrent(account)) onError(transactionFailureMessage(context, e, R.string.delete_failed))
            }
        }
    }
}
