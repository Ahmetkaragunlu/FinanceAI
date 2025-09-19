package com.ahmetkaragunlu.financeai.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    var passwordVisibility by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
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
                tint = Color.White
            )
            OutlinedTextField(
                value = "Email",
                onValueChange = {},
                label = {
                    Text(
                        text = stringResource(R.string.email),
                        color = Color(0xFFaea0e4)
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0)
                    )
                },
                shape = RoundedCornerShape(corner = CornerSize(12.dp)),
            )
            OutlinedTextField(
                value = "Password",
                onValueChange = {},
                label = {
                    Text(
                        text = stringResource(R.string.password),
                        color = Color(0xFFaea0e4)
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0)
                    )
                },
                shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                trailingIcon = {
                    Icon(
                        imageVector = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFcfccf0),
                        modifier = modifier.clickable { passwordVisibility = !passwordVisibility }
                    )
                },
                visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation()
            )
            Text(
                text = stringResource(R.string.forgot_password),
                color = Color(0xFFaea0e4),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(end = 54.dp),
                textAlign = TextAlign.End
            )
            Button(
                onClick = {},
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = 52.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6A11CB), // mor
                                Color(0xFF2575FC)
                            )
                            )
                        ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent // butonun kendi rengini şeffaf yap
                ),
            ) {
                Text(
                    text = stringResource(R.string.login)
                )
            }

        }
    }
}
