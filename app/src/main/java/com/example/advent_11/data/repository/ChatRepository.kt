package com.example.advent_11.data.repository

import com.example.advent_11.api.LlmApiClient
import com.example.advent_11.api.LlmChatMessage
import com.example.advent_11.data.local.ChatDao
import com.example.advent_11.data.local.ChatMessageEntity
import com.example.advent_11.data.local.ChatThreadEntity
import com.example.advent_11.data.local.ChatThreadListItemLocal
import com.example.advent_11.model.ChatMessage
import com.example.advent_11.model.ChatThread
import com.example.advent_11.model.ChatThreadPreview
import com.example.advent_11.model.MessageRole
import com.example.advent_11.model.MessageStatus
import com.example.advent_11.storage.ApiKeyStorage
import com.example.advent_11.storage.LlmSettings
import com.example.advent_11.storage.LlmSettingsStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val apiKeyStorage: ApiKeyStorage,
    private val settingsStorage: LlmSettingsStorage,
    private val llmApiClient: LlmApiClient
) {

    fun observeChatThreads(): Flow<List<ChatThreadPreview>> =
        chatDao.observeChatThreads().map { items -> items.map { it.toDomain() } }

    fun observeChat(chatId: String): Flow<ChatThread?> =
        chatDao.observeChatThread(chatId).map { thread -> thread?.toDomain() }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        chatDao.observeMessages(chatId).map { messages -> messages.map { it.toDomain() } }

    fun hasApiKey(): Boolean = apiKeyStorage.hasApiKey()

    suspend fun createChat(): String {
        val settings = settingsStorage.load()
        val now = System.currentTimeMillis()
        val chatId = UUID.randomUUID().toString()
        chatDao.insertChatThread(
            ChatThreadEntity(
                id = chatId,
                title = DEFAULT_CHAT_TITLE,
                model = settings.model,
                createdAt = now,
                updatedAt = now
            )
        )
        return chatId
    }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
    }

    suspend fun sendMessage(chatId: String, text: String): Result<Unit> {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            return Result.failure(IllegalArgumentException("Сообщение не может быть пустым"))
        }

        val apiKey = apiKeyStorage.getApiKey()?.trim().orEmpty()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Сначала сохраните API ключ"))
        }

        val chatThread = chatDao.getChatThread(chatId)
            ?: return Result.failure(IllegalStateException("Чат не найден"))
        val hadUserMessages = chatDao.getMessages(chatId).any { it.role == MessageRole.USER.value }
        val settings = settingsStorage.load()
        val now = System.currentTimeMillis()

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = MessageRole.USER.value,
            content = trimmedText,
            createdAt = now,
            status = MessageStatus.SENT.value,
            errorMessage = null,
            sequence = nextSequence(chatId)
        )
        val assistantMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = MessageRole.ASSISTANT.value,
            content = "",
            createdAt = now + 1,
            status = MessageStatus.SENDING.value,
            errorMessage = null,
            sequence = userMessage.sequence + 1
        )

        chatDao.insertMessage(userMessage)
        chatDao.insertMessage(assistantMessage)

        val updatedTitle = if (hadUserMessages) {
            chatThread.title
        } else {
            buildChatTitle(trimmedText)
        }
        chatDao.updateChatThread(
            chatThread.copy(
                title = updatedTitle,
                model = settings.model,
                updatedAt = now
            )
        )

        val contextMessages = buildRequestMessages(settings, chatDao.getMessages(chatId))
        return try {
            llmApiClient.sendMessages(
                apiKey = apiKey,
                messages = contextMessages,
                model = settings.model,
                params = settings.toChatRequestParams()
            ).fold(
                onSuccess = { answer ->
                    chatDao.updateMessage(
                        assistantMessage.copy(
                            content = answer,
                            status = MessageStatus.SENT.value,
                            errorMessage = null
                        )
                    )
                    chatDao.updateChatThread(
                        chatThread.copy(
                            title = updatedTitle,
                            model = settings.model,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    Result.success(Unit)
                },
                onFailure = { throwable ->
                    rollbackPendingMessage(
                        chatThread = chatThread,
                        userMessageId = userMessage.id,
                        assistantMessageId = assistantMessage.id
                    )
                    Result.failure(throwable)
                }
            )
        } catch (e: CancellationException) {
            rollbackPendingMessage(
                chatThread = chatThread,
                userMessageId = userMessage.id,
                assistantMessageId = assistantMessage.id
            )
            throw e
        }
    }

    suspend fun retryMessage(chatId: String, messageId: String): Result<Unit> {
        val apiKey = apiKeyStorage.getApiKey()?.trim().orEmpty()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Сначала сохраните API ключ"))
        }

        val chatThread = chatDao.getChatThread(chatId)
            ?: return Result.failure(IllegalStateException("Чат не найден"))
        val message = chatDao.getMessageById(messageId)
            ?: return Result.failure(IllegalStateException("Сообщение не найдено"))
        if (message.role != MessageRole.ASSISTANT.value || message.status != MessageStatus.ERROR.value) {
            return Result.failure(IllegalStateException("Повтор доступен только для ошибочного ответа ИИ"))
        }

        val settings = settingsStorage.load()
        chatDao.updateMessage(
            message.copy(
                content = "",
                status = MessageStatus.SENDING.value,
                errorMessage = null
            )
        )

        val contextMessages = buildRequestMessages(
            settings = settings,
            messages = chatDao.getMessagesBeforeSequence(chatId, message.sequence)
        )
        return llmApiClient.sendMessages(
            apiKey = apiKey,
            messages = contextMessages,
            model = settings.model,
            params = settings.toChatRequestParams()
        ).fold(
            onSuccess = { answer ->
                chatDao.updateMessage(
                    message.copy(
                        content = answer,
                        status = MessageStatus.SENT.value,
                        errorMessage = null
                    )
                )
                chatDao.updateChatThread(
                    chatThread.copy(
                        model = settings.model,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                Result.success(Unit)
            },
            onFailure = { throwable ->
                chatDao.updateMessage(
                    message.copy(
                        content = "",
                        status = MessageStatus.ERROR.value,
                        errorMessage = throwable.message ?: "Не удалось получить ответ"
                    )
                )
                Result.failure(throwable)
            }
        )
    }

    private suspend fun nextSequence(chatId: String): Int = (chatDao.getMaxSequence(chatId) ?: -1) + 1

    private suspend fun rollbackPendingMessage(
        chatThread: ChatThreadEntity,
        userMessageId: String,
        assistantMessageId: String
    ) {
        chatDao.deleteMessages(listOf(userMessageId, assistantMessageId))
        chatDao.updateChatThread(chatThread)
    }

    private fun buildRequestMessages(settings: LlmSettings, messages: List<ChatMessageEntity>): List<LlmChatMessage> {
        val requestMessages = mutableListOf<LlmChatMessage>()
        settings.formatInstruction
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { instruction ->
                requestMessages += LlmChatMessage(
                    role = MessageRole.SYSTEM.value,
                    content = instruction
                )
            }

        messages
            .filter { it.status == MessageStatus.SENT.value }
            .filter { it.role == MessageRole.USER.value || it.role == MessageRole.ASSISTANT.value }
            .filter { it.content.isNotBlank() }
            .forEach { message ->
                requestMessages += LlmChatMessage(
                    role = message.role,
                    content = message.content
                )
            }
        return requestMessages
    }

    private fun buildChatTitle(firstMessage: String): String {
        val compactText = firstMessage.replace("\\s+".toRegex(), " ").trim()
        return compactText.take(MAX_TITLE_LENGTH).ifBlank { DEFAULT_CHAT_TITLE }
    }

    private fun ChatThreadListItemLocal.toDomain(): ChatThreadPreview = ChatThreadPreview(
        id = id,
        title = title,
        lastMessagePreview = lastMessage,
        updatedAt = updatedAt
    )

    private fun ChatThreadEntity.toDomain(): ChatThread = ChatThread(
        id = id,
        title = title,
        model = model,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
        id = id,
        chatId = chatId,
        role = MessageRole.fromValue(role),
        content = content,
        createdAt = createdAt,
        status = MessageStatus.fromValue(status),
        errorMessage = errorMessage,
        sequence = sequence
    )

    companion object {
        const val DEFAULT_CHAT_TITLE = "Новый чат"
        private const val MAX_TITLE_LENGTH = 40
    }
}
