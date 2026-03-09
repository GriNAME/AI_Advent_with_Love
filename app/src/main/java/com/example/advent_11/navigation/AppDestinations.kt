package com.example.advent_11.navigation

sealed class AppDestination(val route: String) {
    data object ChatList : AppDestination("chat_list")
    data object Settings : AppDestination("settings")
    data object Chat : AppDestination("chat/{chatId}") {
        fun createRoute(chatId: String): String = "chat/$chatId"
    }
}
