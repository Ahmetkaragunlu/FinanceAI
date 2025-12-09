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

    // 1. Mesaj Listesi (Room'dan Canlı Akış)
    // stateIn kullanarak Flow'u StateFlow'a çeviriyoruz, böylece Compose ekranı bunu kolayca dinleyebilir.
    // WhileSubscribed(5000): Ekran kapansa bile 5 saniye daha veriyi tutar (döndürme vs. için performans sağlar).
    val chatMessages: StateFlow<List<AiMessageEntity>> = aiRepository.getChatHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Yükleniyor Durumu (Loading)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 3. Mesaj Gönderme Fonksiyonu
    fun sendMessage(text: String) {
        // Boş mesaj gönderilmesini engelle
        if (text.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true

            // Repository'ye işi devret (O hem kaydedecek hem Gemini'ye soracak)
            aiRepository.sendMessage(text)

            _isLoading.value = false
        }
    }
}