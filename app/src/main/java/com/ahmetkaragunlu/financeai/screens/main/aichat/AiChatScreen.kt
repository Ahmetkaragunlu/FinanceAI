package com.ahmetkaragunlu.financeai.screens.main.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity

@Composable
fun AiChatScreen(
    modifier: Modifier = Modifier,
    viewModel: AiViewModel = hiltViewModel(),
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val suggestions = viewModel.suggestionResIds.map { stringResource(it) }

    LaunchedEffect(Unit) {
        viewModel.sendPendingPrompt()
    }
    val initialMessageText = stringResource(R.string.ai_chat_initial_message)
    val displayMessages = remember(messages) {
        messages.ifEmpty {
            listOf(
                AiMessageEntity(
                    id = -1,
                    text = initialMessageText,
                    isAi = true,
                    isSynced = false
                )
            )
        }
    }
    LaunchedEffect(displayMessages.size, viewModel.isLoading) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background))
    ) {
        MessageList(
            messages = displayMessages,
            isLoading = viewModel.isLoading,
            listState = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        SuggestionRow(
            suggestions = suggestions,
            onSuggestionClick = { viewModel.sendMessage(it) }
        )

        ChatInputArea(
            text = viewModel.textState,
            onTextChanged = { viewModel.textState = it },
            onSendClicked = {
                viewModel.sendMessage(viewModel.textState)
                viewModel.textState = ""
            }
        )
    }
}


@Composable
private fun MessageList(
    messages: List<AiMessageEntity>,
    isLoading: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(messages) { message ->
            ChatBubble(message = message)
        }
        if (isLoading) {
            item {
                AiTypingIndicator()
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
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
                onClick = { onSuggestionClick(text) }
            )
        }
    }
}

@Composable
fun ChatBubble(message: AiMessageEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isAi) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (message.isAi) {
            AiAvatarIcon()
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
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
                lineHeight = 20.sp
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
        AiAvatarIcon()
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.ai_chat_loading),
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


@Composable
fun AiAvatarIcon() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF414853)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFF26C6DA),
            modifier = Modifier.size(18.dp)
        )
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
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall,
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
                    style = MaterialTheme.typography.titleSmall
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
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
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = (-2).dp)
            )
        }
    }
}