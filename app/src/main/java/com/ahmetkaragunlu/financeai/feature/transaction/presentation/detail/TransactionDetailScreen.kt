package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.presentation.PhotoSourceBottomSheet
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionActionResult
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.TransactionResultEffect
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component.EditBottomSheet
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component.TransactionDeleteDialog
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component.TransactionDetailActions
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component.TransactionPhotoZoomDialog
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component.TransactionSummaryCard

@Composable
fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    editState: TransactionEditState,
    onEditRequested: () -> Boolean,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategoryChanged: (CategoryType) -> Unit,
    onUpdateRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    actionResult: TransactionActionResult?,
    onResultConsumed: () -> Unit,
    onDeleted: () -> Unit,
    onDeletePhoto: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showEditBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoZoomDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoSourceSheet by rememberSaveable { mutableStateOf(false) }
    var isCategoryDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    TransactionResultEffect(actionResult, onResultConsumed) { result ->
        when (result) {
            TransactionActionResult.Updated -> {
                showEditBottomSheet = false
                Toast.makeText(
                        context,
                        context.getString(R.string.updated_successfully),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
            TransactionActionResult.Deleted -> {
                showDeleteDialog = false
                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT)
                    .show()
                onDeleted()
            }
            TransactionActionResult.PhotoDeleted -> showPhotoZoomDialog = false
            else -> Unit
        }
    }
    val transaction = state.transaction
    if (transaction == null) {
        val description =
            when (state) {
                TransactionDetailUiState.Loading -> stringResource(R.string.transaction_loading)
                TransactionDetailUiState.NotFound -> stringResource(R.string.transaction_not_found)
                is TransactionDetailUiState.Error -> stringResource(state.messageRes)
                is TransactionDetailUiState.Content -> ""
            }
        // Keep the existing empty visual layout while exposing an explicit accessible state.
        Box(
            modifier.fillMaxSize().background(colorResource(R.color.background)).semantics {
                stateDescription = description
            }
        )
        return
    }
    transaction.let { tx ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(color = colorResource(R.color.background))
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Main Card with Gradient
            TransactionSummaryCard(
                tx,
                onPhotoClick = { showPhotoZoomDialog = true },
                onAddPhotoClick = { showPhotoSourceSheet = true },
            )

            TransactionDetailActions(
                onEditRequested = { if (onEditRequested()) showEditBottomSheet = true },
                onDeleteRequested = { showDeleteDialog = true },
                modifier = modifier,
            )
        }

        // --- Photo Zoom Dialog ---
        if (showPhotoZoomDialog && tx.photoUri != null) {
            TransactionPhotoZoomDialog(
                photoUri = tx.photoUri,
                onDismiss = { showPhotoZoomDialog = false },
                onChangePhoto = { showPhotoSourceSheet = true },
                onDeletePhoto = onDeletePhoto,
            )
        }
        // --- Photo Source Bottom Sheet ---
        if (showPhotoSourceSheet) {
            PhotoSourceBottomSheet(
                onDismiss = { showPhotoSourceSheet = false },
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
            )
        }
        // Edit Bottom Sheet
        // A restored UI flag must not reopen a form whose ViewModel draft was lost with the
        // process.
        if (showEditBottomSheet && editState.category != null) {
            EditBottomSheet(
                amount = editState.amount,
                note = editState.note,
                category = editState.category,
                categories = editState.categories,
                onAmountChange = onAmountChanged,
                onNoteChange = onNoteChanged,
                onCategoryChange = onCategoryChanged,
                categoryDropdownExpanded = isCategoryDropdownExpanded,
                onCategoryDropdownExpandedChange = { isCategoryDropdownExpanded = it },
                onDismiss = { showEditBottomSheet = false },
                onSave = onUpdateRequested,
            )
        }
        if (showDeleteDialog) {
            TransactionDeleteDialog(
                onConfirm = onDeleteRequested,
                onDismiss = { showDeleteDialog = false },
            )
        }
    }
}
