package com.example.advent_11.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.advent_11.model.ChatMessage
import com.example.advent_11.model.MessageRole
import com.example.advent_11.model.MessageStatus
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenChatList: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(parameters = { parametersOf(chatId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            // Автопрокрутка нужна, чтобы пользователь сразу видел новый ответ ИИ и состояние отправки.
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenChatList) {
                        Text("Чаты")
                    }
                    TextButton(onClick = onOpenSettings) {
                        Text("Настройки")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                hasApiKey = uiState.hasApiKey,
                inputText = uiState.inputText,
                isSending = uiState.isSending,
                onInputChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onOpenSettings = onOpenSettings
            )
        }
    ) { innerPadding ->
        if (uiState.messages.isEmpty()) {
            EmptyChatState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageItem(
                        message = message,
                        onRetry = { viewModel.retryMessage(message.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Новый чат создан. Отправьте первое сообщение, чтобы начать диалог.",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MessageItem(
    message: ChatMessage,
    onRetry: () -> Unit
) {
    val isUserMessage = message.role == MessageRole.USER
    val horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start
    val cardColors = if (isUserMessage) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = horizontalAlignment
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(if (isUserMessage) 0.9f else 0.95f),
            colors = cardColors
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val text = when {
                    message.status == MessageStatus.SENDING -> "ИИ печатает..."
                    message.content.isNotBlank() -> message.content
                    else -> message.errorMessage ?: "Сообщение недоступно"
                }
                Text(text)

                if (message.status == MessageStatus.ERROR && message.role == MessageRole.ASSISTANT) {
                    Text(
                        text = message.errorMessage ?: "Не удалось получить ответ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = onRetry) {
                        Text("Повторить")
                    }
                }

                Text(
                    text = formatTimestamp(message.createdAt),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    hasApiKey: Boolean,
    inputText: String,
    isSending: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!hasApiKey) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Сохраните API ключ в настройках, чтобы отправлять сообщения.",
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenSettings) {
                        Text("Открыть")
                    }
                }
            }
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Сообщение") },
            minLines = 2,
            maxLines = 5
        )
        Button(
            onClick = onSend,
            modifier = Modifier.fillMaxWidth(),
            enabled = hasApiKey && inputText.isNotBlank() && !isSending
        ) {
            Text(if (isSending) "Отправка..." else "Отправить")
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))
