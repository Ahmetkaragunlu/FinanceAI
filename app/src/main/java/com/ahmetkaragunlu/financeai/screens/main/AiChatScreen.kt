package com.ahmetkaragunlu.financeai.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmetkaragunlu.financeai.R

@Composable
fun AiChatScreen() {
    val dummyMessages = listOf(
        ChatMessage(isAi = true, text = "Merhaba Can! Ekim ayı harcamalarını inceledim.\n\nGıda harcamaların geçen aya göre %15 artmış. Dikkatli olmalısın."),
        ChatMessage(isAi = false, text = "Bu ay ne kadar harcadım?"),
        ChatMessage(isAi = true, text = "Şu ana kadar toplam 15.000 ₺ harcadın.\n\nEn büyük kalemi 5.000 ₺ ile Kira oluşturuyor.")
    )

    val suggestions = listOf("📊 Aylık Özet", "💰 Tasarruf Önerisi", "🚨 Bütçe Riskleri", "🍔 En Çok Nereye Harcadım?")

    var textState by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background))
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dummyMessages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isAi) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.Top
                ) {
                    if (message.isAi) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF414853)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color(0xFF26C6DA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (message.isAi) 0.dp else 16.dp,
                                    bottomEnd = if (message.isAi) 16.dp else 0.dp
                                )
                            )
                            .background(
                                if (message.isAi)
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF3b4351),
                                            Color(0xFF2d3139),
                                            Color(0xFF2d3139)
                                        )
                                    )
                                else
                                    SolidColor(Color(0xFF414853))
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { text ->
                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0xFF414853), RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF414853))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFF414853))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (textState.isEmpty()) {
                    Text("Buraya bir şeyler yaz...", color = Color.LightGray, fontSize = 14.sp)
                }
                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(color = Color(0xFF414853)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Gönder",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = (-2).dp)
                )
            }
        }
    }
}

data class ChatMessage(
    val isAi: Boolean,
    val text: String
)