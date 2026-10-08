package com.ahmetkaragunlu.financeai.feature.auth.presentation.signup

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
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
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceGradients
import com.ahmetkaragunlu.financeai.feature.auth.presentation.component.AuthTextFieldStyles
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.RegistrationFormState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.component.PasswordVisibilityToggle

@Composable
fun SignUpRoute(
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel(),
    onSignIn: () -> Unit,
) {

    val uiState by viewModel.authState.collectAsStateWithLifecycle()
    val failureMessageRes by viewModel.failureMessageRes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler { onSignIn() }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(uiState, lifecycle) {
        lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
            when (uiState) {
                AuthState.FAILURE -> {
                    Toast.makeText(
                            context,
                            context.getString(failureMessageRes ?: R.string.something_went_wrong),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }

                AuthState.VERIFICATION_EMAIL_SENT -> {
                    showDialog = true
                }

                AuthState.VERIFICATION_EMAIL_FAILED -> {
                    Toast.makeText(
                            context,
                            context.getString(R.string.email_verification_could_not_be_sent),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }

                AuthState.USER_ALREADY_EXISTS -> {
                    Toast.makeText(
                            context,
                            context.getString(R.string.this_email_is_already_exists),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }

                else -> {}
            }
            viewModel.resetAuthState()
        }
    }

    ShowDialog(
        visible = showDialog,
        onConfirm = {
            showDialog = false
            viewModel.resetAuthState()
            onSignIn()
        },
    )

    val onSubmitClick: () -> Unit = {
        if (!viewModel.submitRegistration()) {
            Toast.makeText(
                    context,
                    context.getString(R.string.fill_all_fields_correctly),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }
    SignUpScreen(
        form =
            RegistrationFormState(
                email = viewModel.inputEmail,
                password = viewModel.inputPassword,
                firstName = viewModel.inputFirstName,
                lastName = viewModel.inputLastName,
                emailError = viewModel.emailSupportingText(),
                passwordError = viewModel.passwordSupportingText(),
                firstNameError = viewModel.firstNameSupportingText(),
                lastNameError = viewModel.lastNameSupportingText(),
            ),
        onEmailChanged = viewModel::updateEmail,
        onPasswordChanged = viewModel::updatePassword,
        onFirstNameChanged = viewModel::updateFirstName,
        onLastNameChanged = viewModel::updateLastName,
        onSubmitClick = onSubmitClick,
        onSignIn = onSignIn,
        modifier = modifier,
    )
}

@Composable
fun SignUpScreen(
    form: RegistrationFormState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisibility by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(color = colorResource(R.color.background))
    ) {
        Column(
            modifier = modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ai2),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.finance_ai),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.create_an_account),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = modifier.padding(top = 16.dp),
            )
            Spacer(modifier = modifier.height(16.dp))
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EditTextField(
                    value = form.email,
                    onValueChange = onEmailChanged,
                    label = R.string.email,
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Email,
                        ),
                    colors = AuthTextFieldStyles.whiteTextFieldColors(),
                    supportingText = if (form.emailError) R.string.error_email else null,
                )
                EditTextField(
                    value = form.password,
                    onValueChange = onPasswordChanged,
                    label = R.string.password,
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.NumberPassword,
                        ),
                    trailingIcon = {
                        PasswordVisibilityToggle(
                            visible = passwordVisibility,
                            onToggle = { passwordVisibility = !passwordVisibility },
                        )
                    },
                    colors = AuthTextFieldStyles.whiteTextFieldColors(),
                    supportingText = if (form.passwordError) R.string.error_password else null,
                    visualTransformation =
                        if (passwordVisibility) VisualTransformation.None
                        else PasswordVisualTransformation(),
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
                    colors = AuthTextFieldStyles.whiteTextFieldColors(),
                    supportingText = if (form.firstNameError) R.string.error_first_name else null,
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
                    colors = AuthTextFieldStyles.whiteTextFieldColors(),
                    supportingText = if (form.lastNameError) R.string.error_last_name else null,
                )
                Button(
                    onClick = onSubmitClick,
                    modifier =
                        modifier
                            .padding(top = 8.dp)
                            .widthIn(max = 380.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                            .clip(shape = RoundedCornerShape(12.dp))
                            .background(brush = FinanceGradients.authentication),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                ) {
                    Text(text = stringResource(R.string.sign_up))
                }
                TextButton(onClick = { onSignIn() }) {
                    Text(
                        text = stringResource(R.string.already_have_an_account),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowDialog(visible: Boolean, onConfirm: () -> Unit) {
    if (visible) {
        EditAlertDialog(
            title = R.string.email_verification_sent,
            text = R.string.email_diaolog,
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.ok)) }
            },
        )
    }
}
