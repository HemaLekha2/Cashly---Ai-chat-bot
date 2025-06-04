package com.example.smartspendchatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartspendchatbot.screen.BudgetSetupScreen
import com.example.smartspendchatbot.screen.ChatbotScreen
import com.example.smartspendchatbot.viewmodel.BudgetViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            NavHost(navController, startDestination = "setup") {
                composable("setup") {
                    BudgetSetupScreen(
                        viewModel = viewModel,
                        onContinue = { navController.navigate("chatbot") }
                    )
                }
                composable("chatbot") {
                    ChatbotScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}