package com.example.smartspendchatbot.model

// Added isLoading flag for AI responses
data class Message(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false // Default to false
)
