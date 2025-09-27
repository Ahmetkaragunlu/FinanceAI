package com.ahmetkaragunlu.financeai.screens.auth

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.viewmodel.AuthViewModel

@Composable
fun PasswordResetScreen(
    modifier : Modifier = Modifier,
    navController: NavController,
    oobCode : String?,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val whiteColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedIndicatorColor = Color.White,
        unfocusedIndicatorColor = Color.White,
        focusedLabelColor = Color.White
    )
    val context = LocalContext.current
    val uiState by authViewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (uiState) {
            AuthState.SUCCESS -> {
                authViewModel.showDialog = true
            }
            AuthState.INVALID_OOB_CODE -> {
                Toast.makeText(context, context.getString(R.string.your_link_has_expired), Toast.LENGTH_LONG).show()
            }
            AuthState.FAILURE -> {
                Toast.makeText(context, context.getString(R.string.failure), Toast.LENGTH_SHORT).show()
            }
            else -> {}
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
                colors = whiteColors,
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
                supportingText = if (authViewModel.confirmNewPasswordSupportingText()) R.string.confirm_password else null,
                colors = whiteColors,
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
                        authViewModel.confirmPasswordReset(oobCode = oobCode, newPassword = authViewModel.inputNewPassword)
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
            if (authViewModel.showDialog) {
                AlertDialog(
                    onDismissRequest = {  },
                    title = { Text(text = stringResource(R.string.success)) },
                    text = { Text(text = stringResource(R.string.your_password_has_been_changed_successfully)) },
                    confirmButton = {
                        TextButton(onClick = {
                            authViewModel.showDialog = false
                            navController.navigate(Screens.SignInScreen.route)
                        }) {
                            Text(text = stringResource(R.string.ok))
                        }
                    }
                )
            }

        }
    }


}

