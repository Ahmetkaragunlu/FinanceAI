package com.ahmetkaragunlu.financeai.screens.auth

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditAlertDialog
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.ui.theme.TextFieldStyles
import com.ahmetkaragunlu.financeai.viewmodel.AuthViewModel

@Composable
fun PasswordResetRequestScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    navController : NavController
) {

    val context = LocalContext.current
    val uiState by authViewModel.authState.collectAsStateWithLifecycle()
    BackHandler { navController.navigateSingleTopClear(Screens.SignInScreen.route) }
    LaunchedEffect(uiState) {
        when (uiState) {
            AuthState.SUCCESS -> {
                authViewModel.showDialog = true
            }
            AuthState.USER_NOT_FOUND -> {
                Toast.makeText(context, context.getString(R.string.user_not_found) ,Toast.LENGTH_SHORT).show()
            }
            AuthState.FAILURE -> {
                Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
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
                text = stringResource(R.string.forgot_password),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayMedium,
                modifier = modifier.padding(bottom = 36.dp)
            )
            EditTextField(
                value = authViewModel.inputEmail,
                onValueChange = {authViewModel.updateEmail(it)},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                label = R.string.email,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email
                ),
                supportingText = if (authViewModel.emailSupportingText()) R.string.error_email else null,
                colors = TextFieldStyles.whiteTextFieldColors()
            )
            EditTextField(
                value = authViewModel.inputFirstName,
                onValueChange = {authViewModel.updateFirstName(it)},
                label = R.string.first_name,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                supportingText = if (authViewModel.firstNameSupportingText()) R.string.error_firstName else null,
                colors = TextFieldStyles.whiteTextFieldColors()
            )
            EditTextField(
                value = authViewModel.inputLastName,
                onValueChange = {authViewModel.updateLastName(it)},
                label = R.string.last_name,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                supportingText = if (authViewModel.lastNameSupportingText()) R.string.error_lastName else null,
                colors = TextFieldStyles.whiteTextFieldColors()
            )
            Button(
                onClick = {
                    if (authViewModel.isValidResetRequestPassword()) {
                      authViewModel.sendResetPasswordRequest()
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
                    text = stringResource(R.string.send_reset_request)
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
) {
    if(authViewModel.showDialog) {
        EditAlertDialog(
            title = R.string.success,
            text = R.string.reset_request_sent,
            confirmButton = {
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
