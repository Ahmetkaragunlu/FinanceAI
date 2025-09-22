package com.ahmetkaragunlu.financeai.screens.auth

import android.annotation.SuppressLint
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.viewmodel.AuthViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {
    val whiteColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedIndicatorColor = Color.White,
        unfocusedIndicatorColor = Color.White,
        focusedLabelColor = Color.White

    )

    Box(
        modifier =
            modifier
                .fillMaxSize().verticalScroll(rememberScrollState())
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
                    ),
                    colors = whiteColors
                )
                EditTextField(
                    value = authViewModel.inputPassword,
                    onValueChange = {authViewModel.updatePassword(it)},
                    label = R.string.password,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number
                    ),
                    visualTransformation = if(authViewModel.iconVisibility) VisualTransformation.None else PasswordVisualTransformation() ,
                    trailingIcon = {
                        Icon(
                            imageVector = if(authViewModel.iconVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = modifier.clickable{authViewModel.iconVisibility = !authViewModel.iconVisibility}
                        )
                    },
                    colors = whiteColors
                )
                EditTextField(
                    value = authViewModel.inputFirstName,
                    onValueChange = {authViewModel.updateFirstName(it)},
                    label = R.string.first_name,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text
                    ),
                    colors = whiteColors
                )
                EditTextField(
                    value = authViewModel.inputLastName,
                    onValueChange = {authViewModel.updateLastName(it)},
                    label = R.string.last_name,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    colors = whiteColors
                )
                Button(
                    onClick = {},
                    modifier =
                        modifier
                            .padding(top = 8.dp).width(280.dp)
                            .clip(shape = RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFf88b76),
                                        Color(0xFFfea35e)
                                    )
                                )
                            ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)

                ) {
                    Text(
                        text = stringResource(R.string.sign_up)
                    )
                }
               TextButton(
                   onClick = {
                       navController.navigate(Screens.LoginScreen.route)
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

