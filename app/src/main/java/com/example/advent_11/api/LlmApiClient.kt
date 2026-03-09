package com.example.advent_11.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Параметры управления ответом LLM.
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

data class LlmChatMessage(
    val role: String,
    val content: String
)

/**
 * Клиент для DeepSeek API (OpenAI-совместимый формат).
 */
class LlmApiClient(
    private val baseUrl: String = "https://api.deepseek.com/v1"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessages(
        apiKey: String,
        messages: List<LlmChatMessage>,
        model: String,
        params: ChatRequestParams? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (messages.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Нет сообщений для отправки"))
            }

            val requestBody = ChatRequest(
                model = model,
                messages = messages.map { ChatRequest.ChatRequestMessage(role = it.role, content = it.content) },
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

            executeRequest(request)
        } catch (e: CancellationException) {
            Timber.i("LlmApi запрос отменен")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "LlmApi исключение")
            Result.failure(e)
        }
    }

    private suspend fun executeRequest(request: Request): Result<String> =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isActive) return
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!continuation.isActive) {
                        response.close()
                        return
                    }

                    response.use {
                        val body = it.body?.string() ?: ""

                        if (!it.isSuccessful) {
                            Timber.e("LlmApi ошибка: ${it.code} - $body")
                            continuation.resume(Result.failure(Exception("HTTP ${it.code}: $body")))
                            return
                        }

                        val chatResponse = ChatResponse.fromJson(body)
                        val content = chatResponse.choices.firstOrNull()?.message?.content
                        if (content == null) {
                            continuation.resume(Result.failure(Exception("Пустой ответ от API")))
                            return
                        }

                        Timber.d("LlmApi ответ: $content")
                        continuation.resume(Result.success(content.trim()))
                    }
                }
            })
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
