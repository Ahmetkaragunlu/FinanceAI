package com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.ahmetkaragunlu.financeai.core.ui.theme.SignUpTextFieldStyles
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.ResetPasswordFormState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.component.PasswordVisibilityToggle

@Composable
fun PasswordResetRoute(
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit,
    oobCode: String?,
    viewModel: PasswordResetViewModel = hiltViewModel(),
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

                AuthState.FAILURE -> {
                    Toast.makeText(
                            context,
                            context.getString(failureMessageRes ?: R.string.something_went_wrong),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                    viewModel.resetAuthState()
                }

                else -> {}
            }
            viewModel.resetAuthState()
        }
    }
    val onSubmitClick: () -> Unit = {
        if (!viewModel.submitPasswordReset(oobCode)) {
            Toast.makeText(
                    context,
                    context.getString(R.string.fill_all_fields_correctly),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }
    PasswordResetScreen(
        form =
            ResetPasswordFormState(
                password = viewModel.inputNewPassword,
                confirmation = viewModel.inputConfirmPassword,
                passwordError = viewModel.newPasswordSupportingText(),
                confirmationError = viewModel.confirmNewPasswordSupportingText(),
            ),
        onPasswordChanged = viewModel::updateNewPassword,
        onConfirmationChanged = viewModel::updateConfirmPassword,
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
fun PasswordResetScreen(
    form: ResetPasswordFormState,
    onPasswordChanged: (String) -> Unit,
    onConfirmationChanged: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onSignIn: () -> Unit,
    showConfirmation: Boolean,
    onConfirmation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisibility by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisibility by rememberSaveable { mutableStateOf(false) }
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
                text = stringResource(R.string.reset_password),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayMedium,
                modifier = modifier.padding(bottom = 36.dp),
            )
            EditTextField(
                value = form.password,
                onValueChange = onPasswordChanged,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = FinanceColors.mutedText,
                    )
                },
                label = R.string.new_password,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.NumberPassword,
                    ),
                supportingText = if (form.passwordError) R.string.error_password else null,
                colors = SignUpTextFieldStyles.whiteTextFieldColors(),
                trailingIcon = {
                    PasswordVisibilityToggle(
                        visible = passwordVisibility,
                        onToggle = { passwordVisibility = !passwordVisibility },
                    )
                },
                visualTransformation =
                    if (passwordVisibility) VisualTransformation.None
                    else PasswordVisualTransformation(),
            )
            EditTextField(
                value = form.confirmation,
                onValueChange = onConfirmationChanged,
                label = R.string.confirm_password,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.NumberPassword,
                    ),
                supportingText = if (form.confirmationError) R.string.error_password else null,
                colors = SignUpTextFieldStyles.whiteTextFieldColors(),
                trailingIcon = {
                    Icon(
                        imageVector =
                            if (confirmPasswordVisibility) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                        contentDescription =
                            stringResource(
                                if (confirmPasswordVisibility) R.string.hide_password
                                else R.string.show_password
                            ),
                        tint = FinanceColors.resetIcon,
                        modifier =
                            modifier.clickable {
                                confirmPasswordVisibility = !confirmPasswordVisibility
                            },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = FinanceColors.mutedText,
                    )
                },
                visualTransformation =
                    if (confirmPasswordVisibility) VisualTransformation.None
                    else PasswordVisualTransformation(),
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
                Text(text = stringResource(R.string.reset_password))
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
            text = R.string.your_password_has_been_changed_successfully,
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.ok)) }
            },
        )
    }
}
