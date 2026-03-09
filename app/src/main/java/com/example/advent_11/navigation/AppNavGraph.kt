package com.example.advent_11.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.advent_11.ui.LlmSettingsScreen
import com.example.advent_11.ui.chat.ChatScreen
import com.example.advent_11.ui.chatlist.ChatListScreen

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.ChatList.route,
        modifier = modifier
    ) {
        composable(AppDestination.ChatList.route) {
            ChatListScreen(
                onOpenChat = { chatId ->
                    navController.navigate(AppDestination.Chat.createRoute(chatId))
                },
                onOpenSettings = {
                    navController.navigate(AppDestination.Settings.route)
                }
            )
        }
        composable(
            route = AppDestination.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId").orEmpty()
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(AppDestination.Settings.route)
                }
            )
        }
        composable(AppDestination.Settings.route) {
            LlmSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
