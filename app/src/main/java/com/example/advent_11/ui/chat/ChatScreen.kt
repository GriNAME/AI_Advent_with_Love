package com.example.advent_11.ui.chat

import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.advent_11.model.ChatMessage
import com.example.advent_11.model.MessageRole
import com.example.advent_11.model.MessageStatus
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenChatList) {
                        Icon(
                            imageVector = Icons.Outlined.Chat,
                            contentDescription = "Чаты"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Настройки"
                        )
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
                onStop = viewModel::stopSending,
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
                MessageText(
                    text = text,
                    enableSelection = message.role == MessageRole.ASSISTANT || message.role == MessageRole.USER
                )

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
private fun MessageText(
    text: String,
    enableSelection: Boolean
) {
    val formattedText = remember(text) { text.toAnnotatedMarkdown() }
    if (enableSelection) {
        SelectionContainer {
            Text(
                text = formattedText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        Text(
            text = formattedText,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ChatInputBar(
    hasApiKey: Boolean,
    inputText: String,
    isSending: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
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
            enabled = !isSending,
            minLines = 2,
            maxLines = 5
        )
        Button(
            onClick = if (isSending) onStop else onSend,
            modifier = Modifier.fillMaxWidth(),
            enabled = if (isSending) true else hasApiKey && inputText.isNotBlank()
        ) {
            Text(if (isSending) "Стоп" else "Отправить")
        }
    }
}

private fun String.toAnnotatedMarkdown(): AnnotatedString = buildAnnotatedString {
    val lines = normalizeMarkdownText().split("\n")
    lines.forEachIndexed { index, line ->
        val lineStart = length
        val (headingLevel, content) = extractHeading(line)
        appendInlineMarkdown(content)
        val lineEnd = length

        if (headingLevel > 0 && lineStart < lineEnd) {
            addStyle(style = headingStyle(headingLevel), start = lineStart, end = lineEnd)
        }

        if (index != lines.lastIndex) {
            append("\n")
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    var currentIndex = 0
    INLINE_STYLE_REGEX.findAll(text).forEach { match ->
        if (match.range.first > currentIndex) {
            append(text.substring(currentIndex, match.range.first))
        }

        val start = length
        when {
            match.groupValues[1].isNotEmpty() -> {
                append(match.groupValues[1])
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = start,
                    end = length
                )
            }
            match.groupValues[2].isNotEmpty() -> {
                append(match.groupValues[2])
                addStyle(
                    style = SpanStyle(fontStyle = FontStyle.Italic),
                    start = start,
                    end = length
                )
            }
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}

private fun extractHeading(line: String): Pair<Int, String> {
    val match = HEADING_REGEX.find(line) ?: return 0 to line
    val level = match.groupValues[1].length
    return level to line.removeRange(match.range).trimStart()
}

private fun headingStyle(level: Int): SpanStyle = when (level) {
    1 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    2 -> SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
    3 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    else -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

private fun String.normalizeMarkdownText(): String = this
    .replace("\\(", "(")
    .replace("\\)", ")")
    .replace("\\[", "[")
    .replace("\\]", "]")

private val HEADING_REGEX = Regex("^(#{1,6})\\s*")
private val INLINE_STYLE_REGEX = Regex("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*")

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
