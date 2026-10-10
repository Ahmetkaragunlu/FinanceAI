package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.EditTextField
import com.ahmetkaragunlu.financeai.core.ui.component.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.core.ui.component.getAccountCurrencySymbol
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditBottomSheet(
    amount: String,
    note: String,
    category: CategoryType?,
    categories: List<CategoryType>,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCategoryChange: (CategoryType) -> Unit,
    categoryDropdownExpanded: Boolean,
    onCategoryDropdownExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FinanceColors.dialogSurface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.edit_transaction_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            // Amount
            EditTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number,
                    ),
                placeholder = R.string.enter_amount,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = FinanceColors.elevatedSurface,
                        focusedContainerColor = FinanceColors.elevatedSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                trailingIcon = {
                    Text(getAccountCurrencySymbol(), color = MaterialTheme.colorScheme.onPrimary)
                },
            )
            // Category Dropdown
            FinanceDropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                expanded = categoryDropdownExpanded,
                onExpandedChange = onCategoryDropdownExpandedChange,
                options = categories,
                onOptionSelected = { category ->
                    onCategoryChange(category)
                    onCategoryDropdownExpandedChange(false)
                },
                itemLabel = { category -> stringResource(category.toLabelResId()) },
                trigger = {
                    OutlinedTextField(
                        value = category?.let { stringResource(it.toLabelResId()) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.select_category),
                                color = FinanceColors.mutedText,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier =
                                    Modifier.clickable { onCategoryDropdownExpandedChange(true) },
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth().clickable {
                                onCategoryDropdownExpandedChange(true)
                            },
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = FinanceColors.elevatedSurface,
                                disabledTextColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                    )
                },
            )
            // Note
            EditTextField(
                value = note,
                onValueChange = onNoteChange,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text,
                    ),
                placeholder = R.string.enter_your_note,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = FinanceColors.elevatedSurface,
                        focusedContainerColor = FinanceColors.elevatedSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )

            // Save Button
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors =
                    ButtonDefaults.buttonColors(containerColor = FinanceColors.elevatedSurface),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
