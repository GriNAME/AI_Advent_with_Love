package com.example.advent_11

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.advent_11.navigation.AppNavGraph
import com.example.advent_11.ui.theme.Advent_11Theme

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
}

@Composable
private fun MainContent(modifier: Modifier = Modifier) {
    AppNavGraph(modifier = modifier)
}
