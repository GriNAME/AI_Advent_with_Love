package com.example.advent_11.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.advent_11.api.ChatRequestParams
import androidx.core.content.edit

/**
 * Хранилище настроек LLM.
 */
class LlmSettingsStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): LlmSettings = LlmSettings(
        model = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL },
        temperature = prefs.getFloat(KEY_TEMPERATURE, 1f).toDouble(),
        topP = prefs.getFloat(KEY_TOP_P, 1f).toDouble(),
        topK = prefs.getInt(KEY_TOP_K, 0).takeIf { it > 0 },
        maxTokens = prefs.getInt(KEY_MAX_TOKENS, 1024).takeIf { it > 0 },
        seed = prefs.getInt(KEY_SEED, 0).takeIf { it != 0 },
        frequencyPenalty = prefs.getFloat(KEY_FREQUENCY_PENALTY, 0f).toDouble().takeIf { it != 0.0 },
        presencePenalty = prefs.getFloat(KEY_PRESENCE_PENALTY, 0f).toDouble().takeIf { it != 0.0 },
        formatInstruction = prefs.getString(KEY_FORMAT_INSTRUCTION, null)?.takeIf { it.isNotBlank() }
    )

    fun save(settings: LlmSettings) {
        prefs.edit {
            putString(KEY_MODEL, settings.model)
                .putFloat(KEY_TEMPERATURE, settings.temperature.toFloat())
                .putFloat(KEY_TOP_P, settings.topP.toFloat())
                .putInt(KEY_TOP_K, settings.topK ?: 0)
                .putInt(KEY_MAX_TOKENS, settings.maxTokens ?: 1024)
                .putInt(KEY_SEED, settings.seed ?: 0)
                .putFloat(KEY_FREQUENCY_PENALTY, (settings.frequencyPenalty ?: 0.0).toFloat())
                .putFloat(KEY_PRESENCE_PENALTY, (settings.presencePenalty ?: 0.0).toFloat())
                .putString(KEY_FORMAT_INSTRUCTION, settings.formatInstruction ?: "")
        }
    }

    companion object {
        private const val PREFS_NAME = "llm_settings"
        private const val DEFAULT_MODEL = "deepseek-chat"
        private const val KEY_MODEL = "model"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TOP_P = "top_p"
        private const val KEY_TOP_K = "top_k"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_SEED = "seed"
        private const val KEY_FREQUENCY_PENALTY = "frequency_penalty"
        private const val KEY_PRESENCE_PENALTY = "presence_penalty"
        private const val KEY_FORMAT_INSTRUCTION = "format_instruction"
    }
}

/**
 * Настройки параметров LLM.
 */
data class LlmSettings(
    val model: String = "deepseek-chat",
    val temperature: Double = 1.0,
    val topP: Double = 1.0,
    val topK: Int? = null,
    val maxTokens: Int? = null,
    val seed: Int? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val formatInstruction: String? = null,
) {
    fun toChatRequestParams(): ChatRequestParams = ChatRequestParams(
        maxTokens = maxTokens,
        temperature = temperature,
        topP = topP,
        topK = topK,
        frequencyPenalty = frequencyPenalty,
        presencePenalty = presencePenalty,
        seed = seed,
        formatInstruction = formatInstruction
    )
}
