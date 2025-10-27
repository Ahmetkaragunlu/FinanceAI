package com.ahmetkaragunlu.financeai.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.ui.theme.AddTransactionScreenTextFieldStyles
import com.ahmetkaragunlu.financeai.util.getCurrencySymbol
import com.ahmetkaragunlu.financeai.viewmodel.AddTransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.background))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { viewModel.updateTransactionType(TransactionType.EXPENSE) },
                modifier = Modifier.weight(1f),
                colors = if (viewModel.selectedTransactionType == TransactionType.EXPENSE) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
                } else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(R.string.expense),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            OutlinedButton(
                onClick = { viewModel.updateTransactionType(TransactionType.INCOME) },
                modifier = Modifier.weight(1f),
                colors = if (viewModel.selectedTransactionType == TransactionType.INCOME) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
                } else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(R.string.income),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EditTextField(
                value = viewModel.inputAmount,
                onValueChange = { viewModel.updateInputAmount(it) },
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                placeholder = R.string.enter_amount,
                colors = AddTransactionScreenTextFieldStyles.textFieldColors(),
                trailingIcon = {
                    Text(
                        getCurrencySymbol(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )

            CategoryDropdownMenu(
                selectedCategory = viewModel.selectedCategory,
                availableCategories = viewModel.availableCategories,
                expanded = viewModel.isCategoryDropdownExpanded,
                onCategorySelected = { viewModel.updateCategory(it) },
                onToggleDropdown = { viewModel.toggleDropdown() },
                onDismissDropdown = { viewModel.dismissDropdown() },
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth()
            )

            EditTextField(
                value = viewModel.inputNote,
                onValueChange = { viewModel.updateInputNote(it) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                placeholder = R.string.enter_your_note,
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth(),
                colors = AddTransactionScreenTextFieldStyles.textFieldColors()
            )

            DatePickerField(
                selectedDate = viewModel.selectedDate,
                onDateClick = { viewModel.openDatePicker() },
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth()
            )

            ReminderSwitch(
                isEnabled = viewModel.isReminderEnabled,
                onToggle = { viewModel.toggleReminder(it) },
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.saveTransaction(
                        onSuccess = {},
                        onError = {}
                    )
                },
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404349)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(
                        id = if (viewModel.isReminderEnabled)
                        R.string.create_reminder_button else R.string.save_button
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    // DatePicker
    if (viewModel.isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.selectedDate
        )

        DatePickerDialog(
            onDismissRequest = { viewModel.closeDatePicker() },
            colors = DatePickerDefaults.colors(
                containerColor = colorResource(R.color.background),
                dayInSelectionRangeContainerColor = Color.Red


            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            if (viewModel.isDateValid(timestamp)) {
                                viewModel.updateSelectedDate(timestamp)
                                viewModel.closeDatePicker()
                            }
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.ok),
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDatePicker() }) {
                    Text(stringResource(id = R.string.cancel),
                        color = Color.Red)
                }
            }
        ) {

            DatePicker(
                state = datePickerState,
            )
        }
    }
}







@Composable
fun DatePickerField(
    selectedDate: Long,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(selectedDate))
    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        readOnly = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = Color.Gray
            )
        },
        modifier = modifier.clickable { onDateClick() },
        colors = OutlinedTextFieldDefaults.colors(
            disabledContainerColor = Color(0xFF404349),
            disabledTextColor = MaterialTheme.colorScheme.onPrimary
        ),
        enabled = false,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun ReminderSwitch(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color = Color(0xFF404349), shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.reminder_switch_title), color = MaterialTheme.colorScheme.onPrimary)
            Text(
                text = stringResource(
                    id = if (isEnabled) R.string.reminder_enabled_desc
                 else R.string.reminder_disabled_desc),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF26b7c3),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2C2F33)
            )
        )
    }
}








@Composable
fun CategoryDropdownMenu(
    selectedCategory: CategoryType?,
    availableCategories: List<CategoryType>,
    expanded: Boolean,
    onCategorySelected: (CategoryType) -> Unit,
    onToggleDropdown: () -> Unit,
    onDismissDropdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedCategory?.name?.replace("_", " ") ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.select_category),
                    color = Color.Gray
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clickable { onToggleDropdown() }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleDropdown() },
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color(0xFF404349),
                disabledTextColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = false,
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissDropdown,
            modifier = Modifier.background(colorResource(R.color.background))
        ) {
            availableCategories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category.name.replace("_", " "),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        onDismissDropdown()
                    }
                )
            }
        }
    }
}