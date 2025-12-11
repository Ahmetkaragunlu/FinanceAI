package com.ahmetkaragunlu.financeai.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import com.ahmetkaragunlu.financeai.viewmodel.AiViewModel
import kotlinx.coroutines.delay

@Composable
fun AiChatScreen(
    viewModel: AiViewModel = hiltViewModel()
) {
    // ViewModel'den gelen gerçek veriler
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Liste kontrolü
    val listState = rememberLazyListState()

    // ✨ YENİ: Sayfa açıldığında bekleyen prompt varsa gönder
    // Bu işlem bu sayfada tetiklendiği için isLoading bu sayfada aktifleşecek
    // ve "Yazıyor..." animasyonu görünecektir.
    LaunchedEffect(Unit) {
        viewModel.sendPendingPrompt()
    }

    // BOŞ DURUM İÇİN SAHTE MESAJ (GÖRÜNTÜ AMAÇLI)
    val initialMessageText = stringResource(R.string.ai_chat_initial_message)

    // Eğer mesaj listesi boşsa, bu 'hayalet' mesajı içeren bir liste kullan.
    // Doluysa gerçek listeyi kullan.
    val displayMessages = remember(messages) {
        messages.ifEmpty {
            listOf(
                AiMessageEntity(
                    id = -1, // Geçici ID
                    text = initialMessageText,
                    isAi = true,
                    isSynced = false
                )
            )
        }
    }

    // Yeni mesaj gelince en alta kaydır
    LaunchedEffect(displayMessages.size, isLoading) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size)
        }
    }

    val suggestions = listOf(
        stringResource(R.string.ai_suggestion_summary),
        stringResource(R.string.ai_suggestion_saving),
        stringResource(R.string.ai_suggestion_risk),
        stringResource(R.string.ai_suggestion_top_expense)
    )

    var textState by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background))
    ) {
        // Sohbet Alanı
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(displayMessages) { message ->
                ChatBubble(message = message)
            }

            // Yükleniyor animasyonu
            if (isLoading) {
                item {
                    AiTypingIndicator()
                }
            }
        }

        // Öneriler (Chips)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { text ->
                SuggestionChip(
                    text = text,
                    onClick = { viewModel.sendMessage(text) }
                )
            }
        }

        // Input Alanı
        ChatInputArea(
            text = textState,
            onTextChanged = { textState = it },
            onSendClicked = {
                viewModel.sendMessage(textState)
                textState = ""
            }
        )
    }
}

// --- Alt Bileşenler ---

@Composable
fun ChatBubble(message: AiMessageEntity) {
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
                    contentDescription = stringResource(R.string.ai_chat_ai_icon_desc),
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

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
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

@Composable
fun ChatInputArea(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
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
            if (text.isEmpty()) {
                Text(
                    stringResource(R.string.ai_chat_placeholder),
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
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
                .background(color = Color(0xFF414853))
                .clickable { onSendClicked() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = stringResource(R.string.ai_chat_send_desc),
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = (-2).dp)
            )
        }
    }
}

@Composable
fun AiTypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF414853)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Loading",
                tint = Color(0xFF26C6DA),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.ai_chat_loading),
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}