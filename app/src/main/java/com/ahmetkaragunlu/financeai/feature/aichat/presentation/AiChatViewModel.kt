package com.ahmetkaragunlu.financeai.feature.aichat.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage
import com.ahmetkaragunlu.financeai.feature.aichat.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private var pendingAutoPrompt: String? = null
    var textState by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    val suggestionResIds = listOf(
        R.string.ai_suggestion_summary,
        R.string.ai_suggestion_saving,
        R.string.ai_suggestion_risk,
        R.string.ai_suggestion_top_expense
    )
    val chatMessages: StateFlow<List<AiMessage>> = aiRepository.observeChatHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading) return
        isLoading = true
        viewModelScope.launch {
            try {
                aiRepository.sendMessage(text)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                isLoading = false
            }
        }
    }

    fun setPendingPrompt(prompt: String) {
        if (prompt.isNotBlank()) {
            pendingAutoPrompt = prompt
        }
    }

    fun sendPendingPrompt() {
        pendingAutoPrompt?.let { prompt ->
            sendMessage(prompt)
            pendingAutoPrompt = null
        }
    }
}
