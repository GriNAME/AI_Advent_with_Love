package com.example.advent_11.di

import androidx.room.Room
import com.example.advent_11.api.LlmApiClient
import com.example.advent_11.data.local.AppDatabase
import com.example.advent_11.data.repository.ChatRepository
import com.example.advent_11.storage.ApiKeyStorage
import com.example.advent_11.storage.LlmSettingsStorage
import com.example.advent_11.ui.LlmSettingsViewModel
import com.example.advent_11.ui.chat.ChatViewModel
import com.example.advent_11.ui.chatlist.ChatListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModules = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "advent_chat_database"
        ).build()
    }
    single { get<AppDatabase>().chatDao() }
    single { ApiKeyStorage(androidContext()) }
    single { LlmSettingsStorage(androidContext()) }
    single { LlmApiClient() }
    single { ChatRepository(get(), get(), get(), get()) }

    viewModel { ChatListViewModel(get()) }
    viewModel { (chatId: String) -> ChatViewModel(chatId, get()) }
    viewModel { LlmSettingsViewModel(get(), get()) }
}
