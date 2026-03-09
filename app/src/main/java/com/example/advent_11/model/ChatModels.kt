package com.example.advent_11.model

data class ChatThread(
    val id: String,
    val title: String,
    val model: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class ChatThreadPreview(
    val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val updatedAt: Long
)

data class ChatMessage(
    val id: String,
    val chatId: String,
    val role: MessageRole,
    val content: String,
    val createdAt: Long,
    val status: MessageStatus,
    val errorMessage: String?,
    val sequence: Int
)

enum class MessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    companion object {
        fun fromValue(value: String): MessageRole = entries.firstOrNull { it.value == value } ?: USER
    }
}

enum class MessageStatus(val value: String) {
    SENDING("sending"),
    SENT("sent"),
    ERROR("error");

    companion object {
        fun fromValue(value: String): MessageStatus = entries.firstOrNull { it.value == value } ?: SENT
    }
}
