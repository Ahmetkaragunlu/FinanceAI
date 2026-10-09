package com.ahmetkaragunlu.financeai.feature.aichat.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiRequest
import com.ahmetkaragunlu.financeai.feature.aichat.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AiChatViewModel
@Inject
constructor(private val aiRepository: AiRepository, private val session: AccountSession) :
    ViewModel() {

    private var pendingAutoPrompt: String? = null
    var textState by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    private var failedRequest: AiRequest? = null
    private var sendJob: Job? = null
    private val mutableError = MutableStateFlow<Int?>(null)
    val errorResId = mutableError.asStateFlow()
    val chatMessages: StateFlow<List<AiMessage>> =
        aiRepository
            .observeChatHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    init {
        var previous = session.account.value
        viewModelScope.launch {
            session.account.collect { current ->
                if (current != previous) {
                    sendJob?.cancel()
                    sendJob = null
                    isLoading = false
                    textState = ""
                    failedRequest = null
                    pendingAutoPrompt = null
                    mutableError.value = null
                }
                previous = current
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading) return
        val account = session.account.value ?: return
        val request =
            failedRequest?.takeIf { it.ownerId == account.ownerId && it.text == text }
                ?: AiRequest(account.ownerId, UUID.randomUUID().toString(), text)
        isLoading = true
        mutableError.value = null
        if (textState == text) textState = ""
        sendJob =
            viewModelScope.launch {
                try {
                    aiRepository.sendMessage(request)
                    if (session.isCurrent(account)) failedRequest = null
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (session.isCurrent(account)) {
                        failedRequest = request
                        if (textState.isBlank()) textState = text
                        mutableError.value = aiErrorMessageRes(e)
                    }
                } finally {
                    if (session.isCurrent(account)) isLoading = false
                }
            }
    }

    fun sendCurrentMessage() {
        sendMessage(textState)
    }

    fun updateText(text: String) {
        textState = text
    }

    fun dismissError() {
        mutableError.value = null
    }

    fun setPendingPrompt(prompt: String) {
        if (prompt.isNotBlank()) {
            pendingAutoPrompt = prompt
        }
    }

    fun sendPendingPrompt() {
        pendingAutoPrompt?.let { prompt ->
            if (isLoading || session.account.value == null) return
            sendMessage(prompt)
            pendingAutoPrompt = null
        }
    }
}
