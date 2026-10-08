package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.ui.error.dataErrorMessageRes
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.TransactionDetailDestination
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.transactionFailure
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class TransactionDetailViewModel
@Inject
constructor(
    private val photoStore: PhotoLocalStore,
    private val session: AccountSession,
    private val repository: TransactionRepository,
    private val photoWork: PhotoWorkScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    var actionResult by mutableStateOf<TransactionActionResult?>(null)
        private set

    fun consumeActionResult() {
        actionResult = null
    }

    private val transactionId: Int =
        savedStateHandle.toRoute<TransactionDetailDestination>().transactionId
    val uiState: StateFlow<TransactionDetailUiState> =
        repository
            .observeTransactionById(transactionId)
            .map<Transaction?, TransactionDetailUiState> { transaction ->
                transaction?.let(TransactionDetailUiState::Content)
                    ?: TransactionDetailUiState.NotFound
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(
                    TransactionDetailUiState.Error(
                        dataErrorMessageRes(error) ?: R.string.transaction_load_failed
                    )
                )
            }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TransactionDetailUiState.Loading,
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

    fun consumePhotoError() {
        photoErrorResId = null
    }

    val availableCategories: List<CategoryType>
        get() =
            uiState.value.transaction?.let { tx ->
                CategoryType.entries.filter { it.type == tx.transaction }
            } ?: emptyList()

    fun cameraOwnerId(): String? = session.account.value?.ownerId

    fun registerCameraDraft(path: String) {
        clearCameraDraft()
        tempCameraPhotoPath = path
    }

    fun clearCameraDraft() {
        val path = tempCameraPhotoPath
        tempCameraPhotoPath = null
        viewModelScope.launch { photoStore.delete(path) }
    }

    fun onPhotoSelected(uri: Uri) {
        clearCameraDraft()
        updatePhotoInternal(uri, null)
    }

    fun onCameraPhotoTaken() {
        tempCameraPhotoPath?.let { path -> updatePhotoInternal(Uri.fromFile(File(path)), path) }
    }

    private fun updatePhotoInternal(uri: Uri, cameraPath: String?) {
        val currentTx = uiState.value.transaction ?: return
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
                    if (saved == null) {
                        photoErrorResId = R.string.photo_save_failed
                        return@withLock
                    }
                    if (request != photoRequest || !session.isCurrent(account)) return@withLock
                    val previousPhoto = repository.updatePhoto(currentTx, saved)
                    committed = true
                    photoStore.delete(previousPhoto)
                    photoWork.upload(
                        currentTx.ownerId,
                        "transactions",
                        currentTx.firestoreId,
                        saved,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (session.isCurrent(account)) photoErrorResId = R.string.photo_save_failed
                } finally {
                    if (!committed) withContext(NonCancellable) { photoStore.delete(saved) }
                }
            }
        }
    }

    fun deletePhoto() {
        val currentTx = uiState.value.transaction ?: return
        val account = session.account.value?.takeIf { it.ownerId == currentTx.ownerId } ?: return
        photoRequest++
        photoErrorResId = null
        viewModelScope.launch {
            photoUpdates.withLock {
                try {
                    if (!session.isCurrent(account)) return@withLock
                    val previousPhoto = repository.updatePhoto(currentTx, null)
                    photoStore.delete(previousPhoto)
                    if (session.isCurrent(account))
                        actionResult = TransactionActionResult.PhotoDeleted
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (session.isCurrent(account)) photoErrorResId = R.string.photo_delete_failed
                }
            }
        }
    }

    fun prepareEdit(): Boolean {
        val tx = uiState.value.transaction ?: return false
        editAmount = tx.amount.toString()
        editNote = tx.note
        editCategory = tx.category
        return true
    }

    fun updateEditAmount(amount: String) {
        editAmount = amount
    }

    fun updateEditNote(note: String) {
        editNote = note
    }

    fun updateEditCategory(category: CategoryType) {
        editCategory = category
    }

    fun updateTransaction() {
        val currentTransaction = uiState.value.transaction ?: return
        val amount = MoneyAmounts.parse(editAmount, currentTransaction.currencyCode)
        if (amount == null || amount <= 0) {
            actionResult = TransactionActionResult.Failure(R.string.invalid_amount)
            return
        }
        if (editCategory == null) {
            actionResult = TransactionActionResult.Failure(R.string.select_category_error)
            return
        }
        val note = editNote
        val category = editCategory ?: return
        val account =
            session.account.value?.takeIf { it.ownerId == currentTransaction.ownerId } ?: return
        viewModelScope.launch {
            try {
                if (!session.isCurrent(account)) return@launch
                repository.updateDetails(currentTransaction, amount, note, category)

                if (session.isCurrent(account)) actionResult = TransactionActionResult.Updated
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (session.isCurrent(account))
                    actionResult = transactionFailure(e, R.string.update_failed)
            }
        }
    }

    fun deleteTransaction() {
        val currentTransaction = uiState.value.transaction ?: return
        val account =
            session.account.value?.takeIf { it.ownerId == currentTransaction.ownerId } ?: return

        viewModelScope.launch {
            try {
                if (!session.isCurrent(account)) return@launch
                repository.deleteTransaction(currentTransaction)
                photoStore.delete(currentTransaction.photoUri)
                if (session.isCurrent(account)) actionResult = TransactionActionResult.Deleted
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (session.isCurrent(account))
                    actionResult = transactionFailure(e, R.string.delete_failed)
            }
        }
    }
}
