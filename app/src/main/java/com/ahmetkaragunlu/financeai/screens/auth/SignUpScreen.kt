package com.ahmetkaragunlu.financeai.screens.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
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
import com.ahmetkaragunlu.financeai.ui.theme.TextFieldStyles
import com.ahmetkaragunlu.financeai.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {

    val uiState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler {
        navController.popBackStack()
    }

    LaunchedEffect(uiState) {
        when(uiState) {
            AuthState.FAILURE ->  {
                    Toast.makeText(
                        context,
                        context.getString(R.string.something_went_wrong),
                        Toast.LENGTH_SHORT
                    ).show()
            }
            AuthState.VERIFICATION_EMAIL_SENT ->  {
              authViewModel.showDialog = true
            }

            AuthState.VERIFICATION_EMAIL_FAILED ->  {
                Toast.makeText(
                    context,
                    context.getString(R.string.email_verification_could_not_be_sent),
                    Toast.LENGTH_SHORT
                ).show()
            }
            AuthState.USER_ALREADY_EXISTS -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.this_email_is_already_exists),
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {}

        }
        authViewModel.resetAuthState()
    }

    ShowDialog(
        authViewModel = authViewModel,
        navController = navController
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF278ae1),
                            Color(0xFF2acbc0)
                        )
                    )
                )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ai2),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = stringResource(R.string.finance_ai),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimary

            )
            Text(
                text = stringResource(R.string.create_an_account),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = modifier.padding(top = 16.dp)
            )
            Spacer(modifier = modifier.height(16.dp))
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EditTextField(
                    value = authViewModel.inputEmail,
                    onValueChange = { authViewModel.updateEmail(it) },
                    label = R.string.email,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    ),
                    colors = TextFieldStyles.whiteTextFieldColors(),
                    supportingText = if (authViewModel.emailSupportingText()) R.string.error_email else null
                )
                EditTextField(
                    value = authViewModel.inputPassword,
                    onValueChange = { authViewModel.updatePassword(it) },
                    label = R.string.password,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = if (authViewModel.passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = modifier.clickable {
                                authViewModel.passwordVisibility = !authViewModel.passwordVisibility
                            }
                        )
                    },
                    colors = TextFieldStyles.whiteTextFieldColors(),
                    supportingText = if (authViewModel.passwordSupportingText()) R.string.error_password else null,
                    visualTransformation = if (authViewModel.passwordVisibility) VisualTransformation.None else PasswordVisualTransformation()
                )
                EditTextField(
                    value = authViewModel.inputFirstName,
                    onValueChange = { authViewModel.updateFirstName(it) },
                    label = R.string.first_name,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text
                    ),
                    colors = TextFieldStyles.whiteTextFieldColors(),
                    supportingText = if (authViewModel.firstNameSupportingText()) R.string.error_firstName else null
                )
                EditTextField(
                    value = authViewModel.inputLastName,
                    onValueChange = { authViewModel.updateLastName(it) },
                    label = R.string.last_name,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    colors = TextFieldStyles.whiteTextFieldColors(),
                    supportingText = if (authViewModel.lastNameSupportingText()) R.string.error_lastName else null
                )
                Button(
                    onClick = {
                        if (
                            authViewModel.isValidUser()) {
                            authViewModel.saveUser()
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
                                        Color(0xFFf88b76),
                                        Color(0xFFfea35e)
                                    )
                                )
                            ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),

                    ) {
                    Text(
                        text = stringResource(R.string.sign_up)
                    )
                }
                TextButton(
                    onClick = {
                        navController.popBackStack()

                    }
                ) {
                    Text(
                        text = stringResource(R.string.already_have_an_account),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

        }

    }
}








@Composable
fun ShowDialog(
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {
    if (authViewModel.showDialog) {
        EditAlertDialog(
            title =  R.string.email_verification_sent ,
            text = (R.string.email_diaolog) ,
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.showDialog = false
                        authViewModel.resetAuthState()
                        navController.navigate(Screens.SignInScreen.route) {
                            popUpTo(Screens.SignUpScreen.route) { inclusive = true }
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            }
        )
    }
}




