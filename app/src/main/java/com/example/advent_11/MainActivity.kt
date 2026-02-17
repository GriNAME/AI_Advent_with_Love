package com.example.advent_11

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.advent_11.api.LlmApiClient
import com.example.advent_11.storage.ApiKeyStorage
import com.example.advent_11.storage.LlmSettingsStorage
import com.example.advent_11.ui.LlmSettingsScreen
import com.example.advent_11.ui.theme.Advent_11Theme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Advent_11Theme {
                MainContent()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainContent() {
        var showSettings by remember { mutableStateOf(false) }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (showSettings) "Настройки LLM" else "Сравнение ответов") },
                    navigationIcon = {
                        if (showSettings) {
                            IconButton(onClick = { showSettings = false }) {
                                Text("←")
                            }
                        }
                    },
                    actions = {
                        if (!showSettings) {
                            IconButton(onClick = { showSettings = true }) {
                                Text("⚙")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (showSettings) {
                LlmSettingsScreen(
                    onBack = { showSettings = false },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                LlmChatScreen(
                    modifier = Modifier.padding(innerPadding),
                    isVisible = !showSettings
                )
            }
        }
    }
}

@Composable
fun LlmChatScreen(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val context = LocalContext.current
    val apiKeyStorage = remember { ApiKeyStorage(context.applicationContext) }
    val settingsStorage = remember { LlmSettingsStorage(context.applicationContext) }
    var settings by remember { mutableStateOf(settingsStorage.load()) }
    LaunchedEffect(isVisible) {
        if (isVisible) settings = settingsStorage.load()
    }
    var hasStoredKey by remember { mutableStateOf(apiKeyStorage.hasApiKey()) }
    var apiKeyInput by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("В каких случаях 2 умножить на 2 будет 5?") }
    var responseUnrestricted by remember { mutableStateOf("") }
    var responseRestricted by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val unrestrictedParams = settings.toUnrestrictedParams()
    val restrictedParams = settings.toRestrictedParams()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Поле ключа показывается только до его сохранения
        if (!hasStoredKey) {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API ключ DeepSeek") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    if (apiKeyInput.isNotBlank()) {
                        apiKeyStorage.saveApiKey(apiKeyInput.trim())
                        hasStoredKey = true
                        apiKeyInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKeyInput.isNotBlank()
            ) {
                Text("Сохранить ключ")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Без ограничений:", modifier = Modifier.padding(top = 4.dp))
            OutlinedTextField(
                value = responseUnrestricted,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                readOnly = true,
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("С ограничениями (формат, max ${settings.restrictedMaxTokens} токенов, stop=${settings.stopSequence?.ifBlank { "[КОНЕЦ]" } ?: "[КОНЕЦ]"}):", modifier = Modifier.padding(top = 4.dp))
            OutlinedTextField(
                value = responseRestricted,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                readOnly = true,
                minLines = 3
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Промпт (одинаковый для обоих запросов)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Button(
            onClick = {
                val apiKey = apiKeyStorage.getApiKey()
                if (apiKey.isNullOrBlank()) {
                    responseUnrestricted = "Сначала сохраните API ключ"
                    return@Button
                }
                isLoading = true
                responseUnrestricted = ""
                responseRestricted = ""
                scope.launch {
                    val client = LlmApiClient(apiKey = apiKey)
                    val promptText = prompt.trim()
                    val (resultUnrestricted, resultRestricted) = coroutineScope {
                        val def1 = async { client.sendPrompt(promptText, params = unrestrictedParams) }
                        val def2 = async { client.sendPrompt(promptText, params = restrictedParams) }
                        Pair(def1.await(), def2.await())
                    }
                    isLoading = false
                    responseUnrestricted = resultUnrestricted.fold(
                        onSuccess = {
                            Timber.i("Без ограничений: $it")
                            it
                        },
                        onFailure = {
                            Timber.e(it, "Ошибка")
                            "Ошибка: ${it.message}"
                        }
                    )
                    responseRestricted = resultRestricted.fold(
                        onSuccess = {
                            Timber.i("С ограничениями: $it")
                            it
                        },
                        onFailure = {
                            Timber.e(it, "Ошибка")
                            "Ошибка: ${it.message}"
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Загрузка..." else "Сравнить ответы")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
