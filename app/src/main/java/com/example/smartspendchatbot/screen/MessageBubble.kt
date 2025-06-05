package com.example.smartspendchatbot.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// Removed AnnotatedString and related imports as styling is removed
import androidx.compose.ui.unit.dp
import com.example.smartspendchatbot.model.Message

// Function to clean markdown characters using Kotlin Regex
// This function removes symbols like ##, **, *, _ and replaces list markers.
fun cleanMarkdownSyntax(text: String): String {
    return text
        // Remove heading markers
        .replace(Regex("""^##\s+""", RegexOption.MULTILINE), "")
        // Remove bold markers
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        // Remove italic markers (*)
        .replace(Regex("""\*(.*?)\*"""), "$1")
        // Remove italic markers (_)
        .replace(Regex("""_(.*?)_"""), "$1")
        // Replace list markers (*)
        .replace(Regex("""^\*\s+""", RegexOption.MULTILINE), "• ")
        // Replace list markers (-)
        .replace(Regex("""^-\s+""", RegexOption.MULTILINE), "• ")
    // Optional: Clean up numbered lists if needed, or leave as is
    // .replace(Regex("""^\d+\.\s+""", RegexOption.MULTILINE), "$0")
}


@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            color = if (message.isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // *** SIMPLIFIED: Only clean the text, no styling applied ***
                val cleanedText = cleanMarkdownSyntax(message.text)

                Text(
                    text = cleanedText, // Display the cleaned text directly
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )

                if (message.isLoading) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (message.isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
