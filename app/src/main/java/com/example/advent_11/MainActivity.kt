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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.advent_11.ui.theme.Advent_11Theme
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Advent_11Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LlmChatScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun LlmChatScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apiKeyStorage = remember { ApiKeyStorage(context.applicationContext) }
    var hasStoredKey by remember { mutableStateOf(apiKeyStorage.hasApiKey()) }
    var apiKeyInput by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Промпт") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Button(
            onClick = {
                val apiKey = apiKeyStorage.getApiKey()
                if (apiKey.isNullOrBlank()) {
                    response = "Сначала сохраните API ключ"
                    return@Button
                }
                isLoading = true
                response = ""
                scope.launch {
                    val client = LlmApiClient(apiKey = apiKey)
                    val result = client.sendPrompt(prompt.trim())
                    isLoading = false
                    response = result.fold(
                        onSuccess = {
                            Timber.i("Ответ LLM: $it")
                            it
                        },
                        onFailure = {
                            Timber.e(it, "Ошибка LLM")
                            "Ошибка: ${it.message}"
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Загрузка..." else "Отправить в LLM")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Ответ:", modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = response,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            readOnly = true,
            minLines = 5
        )
    }
}
