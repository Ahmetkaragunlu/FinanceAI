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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.app.navigation.Screens
import com.ahmetkaragunlu.financeai.app.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.core.ui.component.EditAlertDialog
import com.ahmetkaragunlu.financeai.core.ui.component.EditTextField
import com.ahmetkaragunlu.financeai.core.ui.theme.SignUpTextFieldStyles
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset.PasswordResetViewModel

@Composable
fun PasswordResetScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    oobCode: String?,
    viewModel: PasswordResetViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.authState.collectAsStateWithLifecycle()
    BackHandler {
        navController.navigateSingleTopClear(Screens.SignInScreen.route)
    }
    LaunchedEffect(uiState) {
        when (uiState) {
            AuthState.SUCCESS -> {
                viewModel.showDialog = true
            }

            AuthState.FAILURE -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.something_went_wrong),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetAuthState()
            }

            else -> {}
        }
        viewModel.resetAuthState()
    }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(
                    color = colorResource(R.color.background)
                )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(top = 240.dp)
        ) {
            Text(
                text = stringResource(R.string.reset_password),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayMedium,
                modifier = modifier.padding(bottom = 36.dp)
            )
            EditTextField(
                value = viewModel.inputNewPassword,
                onValueChange = { viewModel.updateNewPassword(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                label = R.string.new_password,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.NumberPassword
                ),
                supportingText = if (viewModel.newPasswordSupportingText()) R.string.error_password else null,
                colors = SignUpTextFieldStyles.whiteTextFieldColors(),
                trailingIcon = {
                    Icon(
                        imageVector = if (viewModel.passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0),
                        modifier = modifier.clickable {
                            viewModel.passwordVisibility = !viewModel.passwordVisibility
                        }
                    )
                },
                visualTransformation = if (viewModel.passwordVisibility) VisualTransformation.None else PasswordVisualTransformation()
            )
            EditTextField(
                value = viewModel.inputConfirmPassword,
                onValueChange = { viewModel.updateConfirmPassword(it) },
                label = R.string.confirm_password,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.NumberPassword
                ),
                supportingText = if (viewModel.confirmNewPasswordSupportingText()) R.string.error_password else null,
                colors = SignUpTextFieldStyles.whiteTextFieldColors(),
                trailingIcon = {
                    Icon(
                        imageVector = if (viewModel.confirmPasswordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0),
                        modifier = modifier.clickable {
                            viewModel.confirmPasswordVisibility =
                                !viewModel.confirmPasswordVisibility
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                visualTransformation = if (viewModel.confirmPasswordVisibility) VisualTransformation.None else PasswordVisualTransformation()
            )
            Button(
                onClick = {
                    if (viewModel.checkPassword() && oobCode != null && viewModel.isValidResetPassword()) {
                        viewModel.resetPassword(oobCode)
                    } else {
                        Toast.makeText(
                            context, context.getString(R.string.fill_all_fields_correctly),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier =
                    modifier
                        .padding(top = 8.dp)
                        .width(280.dp)
                        .clip(shape = RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404349)),

                ) {
                Text(
                    text = stringResource(R.string.reset_password)
                )
            }
            ShowDialog(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}


@Composable
private fun ShowDialog(
    navController: NavController,
    viewModel: PasswordResetViewModel = hiltViewModel()
) {
    if (viewModel.showDialog) {
        EditAlertDialog(
            title = R.string.success,
            text = R.string.your_password_has_been_changed_successfully,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.showDialog = false
                    navController.navigateSingleTopClear(Screens.SignInScreen.route)
                }) {
                    Text(text = stringResource(R.string.ok))
                }
            },
        )
    }
}
