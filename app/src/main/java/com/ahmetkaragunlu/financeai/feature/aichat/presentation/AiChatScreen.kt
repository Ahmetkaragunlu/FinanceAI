package com.ahmetkaragunlu.financeai.feature.aichat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import com.ahmetkaragunlu.financeai.core.ui.effect.ToastMessageEffect
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage

@Composable
fun AiChatRoute(
    modifier: Modifier = Modifier,
    viewModel: AiChatViewModel = hiltViewModel(),
    initialPrompt: String? = null,
    onPromptConsumed: () -> Unit = {},
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val errorResId by viewModel.errorResId.collectAsStateWithLifecycle()
    ToastMessageEffect(errorResId, viewModel::dismissError)
    LaunchedEffect(initialPrompt, viewModel.isLoading) {
        initialPrompt?.let { prompt ->
            viewModel.setPendingPrompt(prompt)
            onPromptConsumed()
        }
        viewModel.sendPendingPrompt()
    }
    AiChatScreen(
        messages = messages,
        isLoading = viewModel.isLoading,
        text = viewModel.textState,
        onTextChanged = viewModel::updateText,
        onSuggestionClick = viewModel::sendMessage,
        onSendClicked = viewModel::sendCurrentMessage,
        modifier = modifier,
    )
}

@Composable
fun AiChatScreen(
    messages: List<AiMessage>,
    isLoading: Boolean,
    text: String,
    onTextChanged: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onSendClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val suggestions =
        listOf(
                R.string.ai_suggestion_summary,
                R.string.ai_suggestion_saving,
                R.string.ai_suggestion_risk,
                R.string.ai_suggestion_top_expense,
            )
            .map { stringResource(it) }

    val initialMessageText = stringResource(R.string.ai_chat_initial_message)
    val displayMessages =
        remember(messages, initialMessageText) {
            messages.ifEmpty {
                listOf(AiMessage(id = -1, text = initialMessageText, isAi = true, isSynced = false))
            }
        }
    LaunchedEffect(displayMessages.size, isLoading) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colorResource(id = R.color.background))) {
        MessageList(
            messages = displayMessages,
            isLoading = isLoading,
            listState = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        SuggestionRow(suggestions = suggestions, onSuggestionClick = onSuggestionClick)

        ChatInputArea(text = text, onTextChanged = onTextChanged, onSendClicked = onSendClicked)
    }
}

@Composable
private fun MessageList(
    messages: List<AiMessage>,
    isLoading: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = Spacing.screenPadding),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(messages, key = { it.id }) { message -> ChatBubble(message = message) }
        if (isLoading) {
            item { AiTypingIndicator() }
        }
    }
}

@Composable
private fun SuggestionRow(suggestions: List<String>, onSuggestionClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap),
    ) {
        items(suggestions) { text ->
            SuggestionChip(text = text, onClick = { onSuggestionClick(text) })
        }
    }
}

@Composable
fun ChatBubble(message: AiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isAi) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top,
    ) {
        if (message.isAi) {
            AiAvatarIcon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(
            modifier =
                Modifier.widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isAi) 0.dp else 16.dp,
                            bottomEnd = if (message.isAi) 16.dp else 0.dp,
                        )
                    )
                    .background(
                        if (message.isAi)
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        FinanceColors.cardStart,
                                        FinanceColors.cardEnd,
                                        FinanceColors.cardEnd,
                                    )
                            )
                        else SolidColor(FinanceColors.chatSurface)
                    )
                    .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
fun AiTypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiAvatarIcon()
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.ai_chat_loading),
            color = FinanceColors.mutedText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun AiAvatarIcon() {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(FinanceColors.chatSurface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = FinanceColors.aiAccent,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.clickable { onClick() }
                .border(1.dp, FinanceColors.chatSurface, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(FinanceColors.chatSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ChatInputArea(text: String, onTextChanged: (String) -> Unit, onSendClicked: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.screenPadding).padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(FinanceColors.chatSurface)
                    .padding(horizontal = Spacing.screenPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (text.isEmpty()) {
                Text(
                    stringResource(R.string.ai_chat_placeholder),
                    color = Color.LightGray,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
                textStyle =
                    TextStyle(color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier =
                Modifier.size(50.dp)
                    .clip(CircleShape)
                    .background(color = FinanceColors.chatSurface)
                    .clickable { onSendClicked() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp).offset(x = (-2).dp),
            )
        }
    }
}
