package com.example.advent_11.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advent_11.data.repository.ChatRepository
import com.example.advent_11.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val inputText = MutableStateFlow("")
    private val hasApiKey = MutableStateFlow(chatRepository.hasApiKey())
    private val isSending = MutableStateFlow(false)
    private var sendJob: Job? = null
    private var pendingDraft: String? = null

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.observeChat(chatId),
        chatRepository.observeMessages(chatId),
        inputText,
        hasApiKey,
        isSending
    ) { chat, messages, currentInput, currentHasApiKey, currentIsSending ->
        ChatUiState(
            title = chat?.title ?: "Чат",
            model = chat?.model.orEmpty(),
            messages = messages,
            inputText = currentInput,
            hasApiKey = currentHasApiKey,
            isSending = currentIsSending
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
        if (isSending.value) return
        val messageText = inputText.value.trim()
        if (messageText.isBlank()) return

        pendingDraft = messageText
        inputText.value = ""
        isSending.value = true

        sendJob = viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(chatId, messageText)
                if (result.isFailure) {
                    inputText.value = pendingDraft.orEmpty()
                }
                hasApiKey.value = chatRepository.hasApiKey()
            } catch (_: CancellationException) {
                inputText.value = pendingDraft.orEmpty()
            } finally {
                pendingDraft = null
                isSending.value = false
                sendJob = null
            }
        }
    }

    fun stopSending() {
        sendJob?.cancel()
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
