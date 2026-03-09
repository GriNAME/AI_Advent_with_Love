package com.example.advent_11.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advent_11.data.repository.ChatRepository
import com.example.advent_11.model.ChatMessage
import com.example.advent_11.model.MessageRole
import com.example.advent_11.model.MessageStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val inputText = MutableStateFlow("")
    private val hasApiKey = MutableStateFlow(chatRepository.hasApiKey())

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.observeChat(chatId),
        chatRepository.observeMessages(chatId),
        inputText,
        hasApiKey
    ) { chat, messages, currentInput, currentHasApiKey ->
        ChatUiState(
            title = chat?.title ?: "Чат",
            model = chat?.model.orEmpty(),
            messages = messages,
            inputText = currentInput,
            hasApiKey = currentHasApiKey,
            isSending = messages.any { it.role == MessageRole.ASSISTANT && it.status == MessageStatus.SENDING }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState()
    )

    fun onInputChanged(value: String) {
        inputText.value = value
    }

    fun refreshSettings() {
        hasApiKey.value = chatRepository.hasApiKey()
    }

    fun sendMessage() {
        val messageText = inputText.value
        if (messageText.isBlank()) return

        viewModelScope.launch {
            val result = chatRepository.sendMessage(chatId, messageText)
            if (result.isSuccess) {
                inputText.value = ""
            } else {
                hasApiKey.value = chatRepository.hasApiKey()
            }
        }
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.retryMessage(chatId, messageId)
            hasApiKey.value = chatRepository.hasApiKey()
        }
    }
}

data class ChatUiState(
    val title: String = "Чат",
    val model: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val hasApiKey: Boolean = false,
    val isSending: Boolean = false
)
