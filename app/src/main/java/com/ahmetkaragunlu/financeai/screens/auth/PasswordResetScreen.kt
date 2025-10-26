package com.ahmetkaragunlu.financeai.screens.auth

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.ahmetkaragunlu.financeai.components.EditAlertDialog
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.ui.theme.SignUpTextFieldStyles
import com.ahmetkaragunlu.financeai.viewmodel.AuthViewModel

@Composable
fun PasswordResetScreen(
    modifier : Modifier = Modifier,
    navController: NavController,
    oobCode : String?,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by authViewModel.authState.collectAsStateWithLifecycle()
    BackHandler {
      navController.navigateSingleTopClear(Screens.SignInScreen.route)
    }
    LaunchedEffect(uiState) {
        when (uiState) {
            AuthState.SUCCESS -> {
                authViewModel.showDialog = true
            }
            AuthState.FAILURE -> {
                Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
                authViewModel.resetAuthState()
            }
            else->{}
        }
        authViewModel.resetAuthState()
    }
    Box (
        modifier =
            modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF475ce1),
                            Color(0xFF4e91f0)
                        )
                    )
                )
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize().padding(top = 240.dp)
        ) {
            Text(
                text = stringResource(R.string.reset_password),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayMedium,
                modifier = modifier.padding(bottom = 36.dp)
            )
            EditTextField(
                value = authViewModel.inputNewPassword,
                onValueChange = {authViewModel.updateNewPassword(it)},
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
                supportingText = if (authViewModel.newPasswordSupportingText()) R.string.error_password else null,
                colors = SignUpTextFieldStyles.whiteTextFieldColors(),
                trailingIcon = {
                    Icon(
                        imageVector = if (authViewModel.passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0),
                        modifier = modifier.clickable {
                            authViewModel.passwordVisibility = !authViewModel.passwordVisibility
                        }
                    )
                },
                visualTransformation = if (authViewModel.passwordVisibility) VisualTransformation.None else PasswordVisualTransformation()
            )
            EditTextField(
                value = authViewModel.inputConfirmPassword,
                onValueChange = {authViewModel.updateConfirmPassword(it)},
                label = R.string.confirm_password,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.NumberPassword
                ),
                supportingText = if (authViewModel.confirmNewPasswordSupportingText()) R.string.error_password else null,
                colors = SignUpTextFieldStyles.whiteTextFieldColors(),
                trailingIcon = {
                    Icon(
                        imageVector = if (authViewModel.confirmPasswordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0),
                        modifier = modifier.clickable {
                            authViewModel.confirmPasswordVisibility = !authViewModel.confirmPasswordVisibility
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
                visualTransformation = if (authViewModel.confirmPasswordVisibility) VisualTransformation.None else PasswordVisualTransformation()
            )
            Button(
                onClick = {
                    if (authViewModel.checkPassword() && oobCode!=null && authViewModel.isValidResetPassword()) {
                        authViewModel.resetPassword(oobCode)
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
                        .clip(shape = RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF5547e3),
                                    Color(0xFF3a77f5)
                                )
                            )
                        ),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),

                ) {
                Text(
                    text = stringResource(R.string.reset_password)
                )
            }
            ShowDialog(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }
}








@Composable
private fun ShowDialog(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
)
{
    if (authViewModel.showDialog) {
        EditAlertDialog(
            title = R.string.success,
            text = R.string.your_password_has_been_changed_successfully,
            confirmButton =  {
                TextButton(onClick = {
                    authViewModel.showDialog = false
                   navController.navigateSingleTopClear(Screens.SignInScreen.route)
                }) {
                    Text(text = stringResource(R.string.ok))
                }
            },
        )
    }
}

