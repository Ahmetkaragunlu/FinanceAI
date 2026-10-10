package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component.DatePickerField
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component.ReminderSwitch
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component.TransactionAttachments
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component.TransactionInputFields
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component.TransactionTypeSelector

@Composable
fun AddTransactionScreen(
    state: AddTransactionUiState,
    onTypeChanged: (TransactionType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategoryChanged: (CategoryType) -> Unit,
    onNoteChanged: (String) -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onClearLocation: () -> Unit,
    onClearPhoto: () -> Unit,
    onDateClick: () -> Unit,
    onLocationClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .fillMaxSize()
                .background(color = colorResource(R.color.background))
                .verticalScroll(rememberScrollState()),
    ) {
        // Transaction Type Selection
        TransactionTypeSelector(state.type, onTypeChanged)
        Spacer(modifier = modifier.height(Spacing.sectionGap))

        // Form Fields
        Column(
            modifier = modifier.padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TransactionInputFields(
                state.amount,
                state.note,
                state.category,
                state.type,
                onAmountChanged,
                onNoteChanged,
                onCategoryChanged,
            )

            // Date Picker
            DatePickerField(
                selectedDate = state.date,
                onDateClick = { onDateClick() },
                isReminderEnabled = state.reminderEnabled,
                zone = state.zone,
                modifier = modifier.widthIn(max = 450.dp).padding(bottom = 14.dp).fillMaxWidth(),
            )

            // Reminder Switch
            ReminderSwitch(
                isEnabled = state.reminderEnabled,
                onToggle = onReminderChanged,
                modifier = modifier.widthIn(max = 450.dp).padding(bottom = 16.dp).fillMaxWidth(),
            )

            // Location & Photo Cards
            TransactionAttachments(
                state.location,
                state.photoUri,
                onLocationClick,
                onPhotoClick,
                onClearLocation,
                onClearPhoto,
            )
            // Save Button
            Button(
                onClick = onSaveClick,
                modifier = modifier.widthIn(max = 450.dp).fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(
                        id =
                            if (state.reminderEnabled) R.string.create_reminder_button
                            else R.string.save
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
