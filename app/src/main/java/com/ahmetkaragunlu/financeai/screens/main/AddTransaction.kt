package com.ahmetkaragunlu.financeai.screens.transaction

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.ui.theme.AddTransactionScreenTextFieldStyles
import com.ahmetkaragunlu.financeai.utils.CategoryDropdownMenu
import com.ahmetkaragunlu.financeai.utils.DatePickerField
import com.ahmetkaragunlu.financeai.utils.ReminderSwitch
import com.ahmetkaragunlu.financeai.utils.getCurrencySymbol
import com.ahmetkaragunlu.financeai.viewmodel.AddTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val context = LocalContext.current

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
                isRemenderEnabled = viewModel.isReminderEnabled,
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

            Row(
                modifier = modifier
                    .widthIn(max = 450.dp)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Card(
                    onClick = {  },
                    modifier = modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF404349)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = modifier.padding(start = 8.dp)
                        )
                        Spacer(modifier = modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.location_optional),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                 Spacer(modifier = modifier.width(8.dp))
                Card(
                    onClick = {  },
                    modifier = modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF404349)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = modifier.padding(start = 12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.photo_optional),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }


            Button(
                onClick = {
                    viewModel.saveTransaction(
                        onSuccess = {
                            navController.navigateSingleTopClear(Screens.HomeScreen.route)
                            Toast.makeText(
                                context,context.getString(R.string.success), Toast.LENGTH_SHORT).show()
                                    },
                        onError = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
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

    if (viewModel.isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.selectedDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return viewModel.isDateValid(utcTimeMillis)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { viewModel.closeDatePicker() },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF2B2D31)),
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
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDatePicker() }) {
                    Text(stringResource(id = R.string.cancel),
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF2B2D31),
                    dayContentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledDayContentColor = Color.Gray,
                    weekdayContentColor = MaterialTheme.colorScheme.onPrimary,
                    dividerColor = Color(0xFF2B2D31),
                    navigationContentColor =  MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    headlineContentColor =  MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = Color.Gray,
                      todayDateBorderColor = Color.Gray,
                    todayContentColor = Color.Gray
                )
            )
        }
    }
}

