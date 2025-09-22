package com.ahmetkaragunlu.financeai.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    Box(
        modifier = modifier
            .fillMaxSize().verticalScroll(rememberScrollState())
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4F46E5),
                        Color(0xFF9333EA)
                    ),
                )
            )
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ai),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            EditTextField(
                value = authViewModel.inputEmail,
                onValueChange = { authViewModel.updateEmail(it) },
                label = R.string.email,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0)
                    )
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                ),
                colors = OutlinedTextFieldDefaults.colors(focusedLabelColor = Color.White)
            )
            EditTextField(
                value = authViewModel.inputPassword,
                onValueChange = { authViewModel.updatePassword(it) },
                label = R.string.password,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = if (authViewModel.iconVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0),
                        modifier = modifier.clickable {
                            authViewModel.iconVisibility = !authViewModel.iconVisibility
                        }
                    )
                },
                visualTransformation = if (authViewModel.iconVisibility) VisualTransformation.None else PasswordVisualTransformation()

            )
            Text(
                text = stringResource(R.string.forgot_password),
                color = Color(0xFFaea0e4),
                modifier = modifier.width(280.dp),
                textAlign = TextAlign.End
            )
            Button(
                onClick = {},
                modifier =
                    modifier
                        .width(280.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6A11CB),
                                    Color(0xFF2575FC)
                                )
                            )
                        ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
            ) {
                Text(
                    text = stringResource(R.string.login)
                )
            }

        }
    }
}
