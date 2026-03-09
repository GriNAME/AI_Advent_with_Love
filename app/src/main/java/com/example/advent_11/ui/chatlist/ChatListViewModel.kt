package com.example.advent_11.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advent_11.data.repository.ChatRepository
import com.example.advent_11.model.ChatThreadPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val uiState: StateFlow<ChatListUiState> = chatRepository.observeChatThreads()
        .map { chats -> ChatListUiState(chats = chats) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatListUiState()
        )

    suspend fun createChat(): String = chatRepository.createChat()

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
        }
    }
}

data class ChatListUiState(
    val chats: List<ChatThreadPreview> = emptyList()
)
