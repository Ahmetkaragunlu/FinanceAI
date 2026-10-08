package com.ahmetkaragunlu.financeai.feature.auth.presentation.signin

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withStateAtLeast
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.EditTextField
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceGradients
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.auth.data.credential.GoogleCredentialSelector
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.component.PasswordVisibilityToggle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SignInRoute(
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
    onSignedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.authState.collectAsStateWithLifecycle()
    val failureMessageRes by viewModel.failureMessageRes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val credentialSelector =
        remember(context) { GoogleCredentialSelector(CredentialManager.create(context)) }
    var selectingGoogleAccount by remember { mutableStateOf(false) }
    // The existing root-screen back policy is explicitly retained.
    BackHandler {}

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(uiState, lifecycle) {
        lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
            when (uiState) {
                AuthState.SUCCESS -> {
                    onSignedIn()
                }

                AuthState.EMAIL_NOT_VERIFIED -> {
                    Toast.makeText(
                            context,
                            context.getString(R.string.please_verify_your_email),
                            Toast.LENGTH_LONG,
                        )
                        .show()
                }

                AuthState.USER_NOT_FOUND -> {
                    Toast.makeText(
                            context,
                            context.getString(R.string.user_not_found),
                            Toast.LENGTH_LONG,
                        )
                        .show()
                }

                AuthState.INVALID_CREDENTIALS -> {
                    Toast.makeText(
                            context,
                            context.getString(R.string.invalid_email_or_password),
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

    val onGoogleSignInClick: () -> Unit = {
        if (!selectingGoogleAccount) {
            selectingGoogleAccount = true
            scope.launch {
                try {
                    val identity =
                        credentialSelector.select(
                            context,
                            context.getString(R.string.default_web_client_id),
                        )
                    viewModel.signInWithGoogle(identity)
                } catch (_: GetCredentialCancellationException) {
                    // Closing the provider chooser does not create a login failure.
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    Toast.makeText(
                            context,
                            context.getString(failureMessageRes ?: R.string.something_went_wrong),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                } finally {
                    selectingGoogleAccount = false
                }
            }
        }
    }
    SignInScreen(
        email = viewModel.inputEmail,
        password = viewModel.inputPassword,
        onEmailChanged = viewModel::updateEmail,
        onPasswordChanged = viewModel::updatePassword,
        onLoginClick = viewModel::login,
        onGoogleSignInClick = onGoogleSignInClick,
        onForgotPassword = {
            onForgotPassword()
            viewModel.clearSignInFields()
        },
        onSignUp = {
            onSignUp()
            viewModel.clearSignInFields()
        },
        modifier = modifier,
    )
}

@Composable
fun SignInScreen(
    email: String,
    password: String,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
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
            modifier = modifier.fillMaxSize().padding(vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        ) {
            Icon(
                painter = painterResource(R.drawable.ai),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )

            EditTextField(
                value = email,
                onValueChange = onEmailChanged,
                label = R.string.email,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email,
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = FinanceColors.onAccent,
                        focusedTextColor = FinanceColors.onAccent,
                        focusedBorderColor = FinanceColors.onAccent,
                    ),
            )

            EditTextField(
                value = password,
                onValueChange = onPasswordChanged,
                label = R.string.password,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Number,
                    ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                trailingIcon = {
                    PasswordVisibilityToggle(
                        visible = passwordVisibility,
                        onToggle = { passwordVisibility = !passwordVisibility },
                    )
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                visualTransformation =
                    if (passwordVisibility) VisualTransformation.None
                    else PasswordVisualTransformation(),
            )
            Row(
                modifier = modifier.widthIn(max = 380.dp).fillMaxWidth().padding(end = 48.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(R.string.forgot_password),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = modifier.clickable { onForgotPassword() },
                )
            }

            Button(
                onClick = { onLoginClick() },
                modifier =
                    modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .background(brush = FinanceGradients.authentication),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            ) {
                Text(text = stringResource(R.string.login))
            }
            GoogleSignInButton(onClick = onGoogleSignInClick, modifier = modifier)
            TextButton(onClick = { onSignUp() }) {
                Text(
                    text = stringResource(R.string.have_an_account_sign_up),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier =
            modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 56.dp)
                .padding(top = 8.dp, bottom = 36.dp)
                .clip(shape = RoundedCornerShape(12.dp)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.google),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Text(
            text = stringResource(R.string.sign_in_with_google),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
