package com.example.advent_11.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Клиент для DeepSeek API (OpenAI-совместимый формат).
 */
class LlmApiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com/v1"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendPrompt(prompt: String, model: String = "deepseek-chat"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = ChatRequest(
                model = model,
                messages = listOf(ChatRequest.ChatRequestMessage(role = "user", content = prompt))
            ).toJson()

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Timber.e("LlmApi ошибка: ${response.code} - $body")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }

            val chatResponse = ChatResponse.fromJson(body)
            val content = chatResponse.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("Пустой ответ от API"))

            Timber.d("LlmApi ответ: $content")
            Result.success(content)
        } catch (e: Exception) {
            Timber.e(e, "LlmApi исключение")
            Result.failure(e)
        }
    }
}

// DTO для запроса (OpenAI Chat Completions API)
private data class ChatRequest(
    val model: String,
    val messages: List<ChatRequestMessage>
) {
    data class ChatRequestMessage(val role: String, val content: String)

    fun toJson(): String = Gson().toJson(this)
}

// DTO для ответа OpenAI API
private data class ChatResponse(
    val choices: List<Choice>
) {
    data class Choice(val message: Message)
    data class Message(val content: String)

    companion object {
        private val gson = Gson()

        fun fromJson(json: String): ChatResponse =
            gson.fromJson(json, ChatResponse::class.java)
    }
}
