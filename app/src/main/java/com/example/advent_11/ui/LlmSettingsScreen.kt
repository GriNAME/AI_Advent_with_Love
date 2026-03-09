package com.example.advent_11.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LlmSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Настройки LLM") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        SettingsContent(
            innerPadding = innerPadding,
            uiState = uiState,
            onApiKeyChange = viewModel::updateApiKey,
            onModelChange = viewModel::updateModel,
            onTemperatureChange = { viewModel.updateTemperature(it.toDouble()) },
            onTopPChange = { viewModel.updateTopP(it.toDouble()) },
            onTopKChange = viewModel::updateTopK,
            onMaxTokensChange = viewModel::updateMaxTokens,
            onSeedChange = viewModel::updateSeed,
            onFrequencyPenaltyChange = { viewModel.updateFrequencyPenalty(it?.toDouble()) },
            onPresencePenaltyChange = { viewModel.updatePresencePenalty(it?.toDouble()) },
            onFormatInstructionChange = viewModel::updateFormatInstruction,
            onSave = { viewModel.save(onBack) }
        )
    }
}

@Composable
private fun SettingsContent(
    innerPadding: PaddingValues,
    uiState: LlmSettingsUiState,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float) -> Unit,
    onTopKChange: (Int?) -> Unit,
    onMaxTokensChange: (Int?) -> Unit,
    onSeedChange: (Int?) -> Unit,
    onFrequencyPenaltyChange: (Float?) -> Unit,
    onPresencePenaltyChange: (Float?) -> Unit,
    onFormatInstructionChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val settings = uiState.settings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("API ключ")
                Spacer(modifier = Modifier.height(8.dp))
                Text(if (uiState.hasStoredKey) "Ключ уже сохранен. При вводе нового значения он будет заменен." else "Ключ пока не сохранен.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.apiKeyInput,
                    onValueChange = onApiKeyChange,
                    label = { Text("API ключ DeepSeek") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Модель")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.model,
                    onValueChange = onModelChange,
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Параметры сэмплирования")
                Spacer(modifier = Modifier.height(8.dp))
                SliderRow("Temperature (0-2)", settings.temperature, 0f..2f, onTemperatureChange)
                SliderRow("Top P (0-1)", settings.topP, 0f..1f, onTopPChange)
                IntOptionalRow("Top K (0 = выкл)", settings.topK, onTopKChange)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Штрафы")
                Spacer(modifier = Modifier.height(8.dp))
                SliderRow("Frequency penalty (-2-2)", settings.frequencyPenalty ?: 0.0, -2f..2f) {
                    onFrequencyPenaltyChange(it.takeIf { value -> value != 0f })
                }
                SliderRow("Presence penalty (-2-2)", settings.presencePenalty ?: 0.0, -2f..2f) {
                    onPresencePenaltyChange(it.takeIf { value -> value != 0f })
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ограничение длины и сид")
                Spacer(modifier = Modifier.height(8.dp))
                IntOptionalRow("Max tokens", settings.maxTokens, onMaxTokensChange)
                IntOptionalRow("Seed (0 = выкл)", settings.seed, onSeedChange)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("System instruction")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.formatInstruction.orEmpty(),
                    onValueChange = onFormatInstructionChange,
                    label = { Text("Инструкция для system message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить")
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Double,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${"%.2f".format(value)}")
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors()
        )
    }
}

@Composable
private fun IntOptionalRow(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit
) {
    var text by remember(value) { mutableStateOf(value?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it.toIntOrNull())
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
