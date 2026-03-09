package com.example.advent_11.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advent_11.storage.ApiKeyStorage
import com.example.advent_11.storage.LlmSettings
import com.example.advent_11.storage.LlmSettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LlmSettingsViewModel(
    private val settingsStorage: LlmSettingsStorage,
    private val apiKeyStorage: ApiKeyStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(LlmSettingsUiState())
    val uiState: StateFlow<LlmSettingsUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _uiState.value = LlmSettingsUiState(
            settings = settingsStorage.load(),
            apiKeyInput = "",
            hasStoredKey = apiKeyStorage.hasApiKey()
        )
    }

    fun updateModel(value: String) = updateSettings { copy(model = value) }
    fun updateTemperature(value: Double) = updateSettings { copy(temperature = value) }
    fun updateTopP(value: Double) = updateSettings { copy(topP = value) }
    fun updateTopK(value: Int?) = updateSettings { copy(topK = value?.takeIf { it > 0 }) }
    fun updateMaxTokens(value: Int?) = updateSettings { copy(maxTokens = value?.takeIf { it > 0 }) }
    fun updateSeed(value: Int?) = updateSettings { copy(seed = value?.takeIf { it != 0 }) }
    fun updateFrequencyPenalty(value: Double?) = updateSettings { copy(frequencyPenalty = value?.takeIf { it != 0.0 }) }
    fun updatePresencePenalty(value: Double?) = updateSettings { copy(presencePenalty = value?.takeIf { it != 0.0 }) }
    fun updateFormatInstruction(value: String) = updateSettings { copy(formatInstruction = value.ifBlank { null }) }

    fun updateApiKey(value: String) {
        _uiState.update { current -> current.copy(apiKeyInput = value) }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            settingsStorage.save(state.settings)
            state.apiKeyInput.trim()
                .takeIf { it.isNotEmpty() }
                ?.let { apiKeyStorage.saveApiKey(it) }
            reload()
            onDone()
        }
    }

    private fun updateSettings(transform: LlmSettings.() -> LlmSettings) {
        _uiState.update { current ->
            current.copy(settings = current.settings.transform())
        }
    }
}

data class LlmSettingsUiState(
    val settings: LlmSettings = LlmSettings(),
    val apiKeyInput: String = "",
    val hasStoredKey: Boolean = false
)
