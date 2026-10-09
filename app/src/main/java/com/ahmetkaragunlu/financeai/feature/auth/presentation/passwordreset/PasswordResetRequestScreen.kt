package com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withStateAtLeast
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.EditAlertDialog
import com.ahmetkaragunlu.financeai.core.ui.component.EditTextField
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.ResetRequestFormState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.component.AuthTextFieldStyles

@Composable
fun PasswordResetRequestRoute(
    modifier: Modifier = Modifier,
    viewModel: PasswordResetRequestViewModel = hiltViewModel(),
    onSignIn: () -> Unit,
) {

    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.authState.collectAsStateWithLifecycle()
    val failureMessageRes by viewModel.failureMessageRes.collectAsStateWithLifecycle()
    BackHandler { onSignIn() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(uiState, lifecycle) {
        lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
            when (uiState) {
                AuthState.SUCCESS -> {
                    showDialog = true
                }

                AuthState.USER_NOT_FOUND -> {
                    Toast.makeText(
                            context,
                            context.getString(R.string.user_not_found),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }

                AuthState.FAILURE -> {
                    Toast.makeText(
                            context,
                            context.getString(failureMessageRes ?: R.string.something_went_wrong),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }

                else -> {}
            }
            viewModel.resetAuthState()
        }
    }

    val onSubmitClick: () -> Unit = {
        if (!viewModel.submitResetRequest()) {
            Toast.makeText(
                    context,
                    context.getString(R.string.fill_all_fields_correctly),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }
    PasswordResetRequestScreen(
        form =
            ResetRequestFormState(
                email = viewModel.inputEmail,
                firstName = viewModel.inputFirstName,
                lastName = viewModel.inputLastName,
                emailError = viewModel.shouldShowEmailError(),
                firstNameError = viewModel.shouldShowFirstNameError(),
                lastNameError = viewModel.shouldShowLastNameError(),
            ),
        onEmailChanged = viewModel::updateEmail,
        onFirstNameChanged = viewModel::updateFirstName,
        onLastNameChanged = viewModel::updateLastName,
        onSubmitClick = onSubmitClick,
        onSignIn = onSignIn,
        showConfirmation = showDialog,
        onConfirmation = {
            showDialog = false
            onSignIn()
        },
        modifier = modifier,
    )
}

@Composable
fun PasswordResetRequestScreen(
    form: ResetRequestFormState,
    onEmailChanged: (String) -> Unit,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onSignIn: () -> Unit,
    showConfirmation: Boolean,
    onConfirmation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(color = colorResource(R.color.background))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
            modifier = modifier.fillMaxSize().padding(top = 240.dp),
        ) {
            Text(
                text = stringResource(R.string.forgot_password),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayMedium,
                modifier = modifier.padding(bottom = 36.dp),
            )
            EditTextField(
                value = form.email,
                onValueChange = onEmailChanged,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = null,
                        tint = FinanceColors.mutedText,
                    )
                },
                label = R.string.email,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email,
                    ),
                supportingText = if (form.emailError) R.string.error_email else null,
                colors = AuthTextFieldStyles.whiteTextFieldColors(),
            )
            EditTextField(
                value = form.firstName,
                onValueChange = onFirstNameChanged,
                label = R.string.first_name,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text,
                    ),
                supportingText = if (form.firstNameError) R.string.error_first_name else null,
                colors = AuthTextFieldStyles.whiteTextFieldColors(),
            )
            EditTextField(
                value = form.lastName,
                onValueChange = onLastNameChanged,
                label = R.string.last_name,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text,
                    ),
                supportingText = if (form.lastNameError) R.string.error_last_name else null,
                colors = AuthTextFieldStyles.whiteTextFieldColors(),
            )
            Button(
                onClick = onSubmitClick,
                modifier =
                    modifier
                        .padding(top = 8.dp)
                        .width(280.dp)
                        .clip(shape = RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.elevatedSurface),
            ) {
                Text(text = stringResource(R.string.send_reset_request))
            }
            ShowDialog(visible = showConfirmation, onConfirm = onConfirmation)
        }
    }
}

@Composable
private fun ShowDialog(visible: Boolean, onConfirm: () -> Unit) {
    if (visible) {
        EditAlertDialog(
            title = R.string.success,
            text = R.string.reset_request_sent,
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.ok)) }
            },
        )
    }
}
