package com.ahmetkaragunlu.financeai.screens.main.aichat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.ai_repository.AiRepository
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    companion object {
        var pendingAutoPrompt: String? = null
    }
    var textState by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    val suggestionResIds = listOf(
        R.string.ai_suggestion_summary,
        R.string.ai_suggestion_saving,
        R.string.ai_suggestion_risk,
        R.string.ai_suggestion_top_expense
    )
    val chatMessages: StateFlow<List<AiMessageEntity>> = aiRepository.getChatHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            isLoading = true
            try {
                aiRepository.sendMessage(text)
            } catch (e: Exception) {
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