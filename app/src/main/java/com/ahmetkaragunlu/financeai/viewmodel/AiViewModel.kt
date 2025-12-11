package com.ahmetkaragunlu.financeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.ai_repository.AiRepository
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    // Veri taşıyıcı (Static/Global gibi davranır, sayfalar arası iletişim için)
    companion object {
        var pendingAutoPrompt: String? = null
    }

    // Mesaj Listesi
    val chatMessages: StateFlow<List<AiMessageEntity>> = aiRepository.getChatHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Yükleniyor Durumu (Loading)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Mesaj Gönderme Fonksiyonu
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                aiRepository.sendMessage(text)
            } catch (e: Exception) {
                // Hata durumunda yapılacaklar (opsiyonel log vs.)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Anasayfadan çağrılacak: Sadece prompt'u kaydet, hemen gönderme!
    fun setPendingPrompt(prompt: String) {
        if (prompt.isNotBlank()) {
            pendingAutoPrompt = prompt
        }
    }

    // Chat sayfasından çağrılacak: Bekleyen varsa gönder ve sil
    // Bu sayede Chat sayfası açıldığında fonksiyon çalışır ve loading bu sayfada görünür.
    fun sendPendingPrompt() {
        pendingAutoPrompt?.let { prompt ->
            sendMessage(prompt)
            pendingAutoPrompt = null // Tekrar tekrar göndermemesi için temizle
        }
    }
}