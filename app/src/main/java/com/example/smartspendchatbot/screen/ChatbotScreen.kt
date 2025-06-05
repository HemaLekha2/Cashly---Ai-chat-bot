package com.example.smartspendchatbot.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.smartspendchatbot.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    viewModel: BudgetViewModel,
    onBack: () -> Unit
) {
    val messages = viewModel.chatMessages
    var userInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isLoading = viewModel.isLoadingAiResponse.value

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                // Scroll to the last item
                scrollState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Spend Bot") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Setup"
                        )
                    }
                },
                actions = {
                    // Analyze Spending action using BarChart icon
                    IconButton(onClick = {
                        viewModel.sendUserMessage("Analyze my spending")
                    }, enabled = !isLoading) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Analyze Spending")
                    }
                    // Get Budget Plan action using MonetizationOn icon
                    IconButton(onClick = {
                        viewModel.sendUserMessage("Give me a budget plan")
                    }, enabled = !isLoading) {
                        Icon(Icons.Filled.Build, contentDescription = "Get Budget Plan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                state = scrollState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(msg)
                }
            }

            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask for plan, analysis, or advice...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (userInput.isNotBlank() && !isLoading) {
                            viewModel.sendUserMessage(userInput)
                            userInput = ""
                            focusManager.clearFocus()
                        }
                    }),
                    leadingIcon = {
                        Icon(Icons.Filled.Info, contentDescription = null)
                    },
                    enabled = !isLoading
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (userInput.isNotBlank() && !isLoading) {
                            viewModel.sendUserMessage(userInput)
                            userInput = ""
                            focusManager.clearFocus()
                        }
                    },
                    enabled = !isLoading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}