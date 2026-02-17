package com.example.advent_11.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Параметры управления ответом LLM.
 * @param maxTokens ограничение длины ответа в токенах
 * @param stop последовательности, при встрече которых генерация останавливается
 * @param temperature случайность (0–2), ниже = детерминированнее
 * @param topP nucleus sampling (0–1)
 * @param topK top-k sampling (некоторые API)
 * @param formatInstruction инструкция формата ответа (добавляется в system message)
 */
data class ChatRequestParams(
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val seed: Int? = null,
    val formatInstruction: String? = null
)

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

    suspend fun sendPrompt(
        prompt: String,
        model: String = "deepseek-chat",
        params: ChatRequestParams? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = buildList {
                params?.formatInstruction?.let { instruction ->
                    add(ChatRequest.ChatRequestMessage(role = "system", content = instruction))
                }
                add(ChatRequest.ChatRequestMessage(role = "user", content = prompt))
            }
            val requestBody = ChatRequest(
                model = model,
                messages = messages,
                maxTokens = params?.maxTokens,
                stop = params?.stop,
                temperature = params?.temperature,
                topP = params?.topP,
                topK = params?.topK,
                frequencyPenalty = params?.frequencyPenalty,
                presencePenalty = params?.presencePenalty,
                seed = params?.seed
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
    val messages: List<ChatRequestMessage>,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val temperature: Double? = null,
    @SerializedName("top_p") val topP: Double? = null,
    @SerializedName("top_k") val topK: Int? = null,
    @SerializedName("frequency_penalty") val frequencyPenalty: Double? = null,
    @SerializedName("presence_penalty") val presencePenalty: Double? = null,
    val seed: Int? = null
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
