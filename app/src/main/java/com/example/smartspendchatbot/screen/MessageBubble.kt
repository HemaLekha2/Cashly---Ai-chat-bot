package com.example.smartspendchatbot.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.smartspendchatbot.model.Message
import android.util.Log

// Helper function to create a simplified key for comparing lines
fun createLineContentKey(line: String): String {
    return line
        .replace(Regex("""\*\*|\*|_"""), "") // Remove markdown bold/italic symbols
        .replace(Regex("""^\s*[*\-•#]+\s*"""), "") // Remove list markers and heading markers
        // Updated regex slightly to be more robust and include leading/trailing space capture
        .replace(Regex("""^(\s*)([^:]+):\2[:\*\s]*"""), "$1$2: ") // Collapse "Key:Key:*: " into "Key: "
        .replace(Regex("""\s+"""), " ") // Normalize internal whitespace
        .trim() // Trim external whitespace after processing
}

// Function to parse basic Markdown to AnnotatedString
@Composable
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString { // <-- CHANGED RETURN TYPE TO AnnotatedString
    // --- Start: Pre-processing to remove duplicate lines --- //
    val originalLines = text.lines()
    val uniqueLines = mutableListOf<String>()
    var previousLineKey: String? = null

    for (line in originalLines) {
        val trimmedLine = line.trim()

        // Keep actual content lines, skip empty lines between them unless significant gap
        if (trimmedLine.isEmpty()) {
            // Optional: add logic to keep only single empty lines, or remove all
            if (uniqueLines.isNotEmpty() && !uniqueLines.last().endsWith("\n\n")) { // Avoid adding multiple blank lines
                // Keep the blank line for spacing, and reset previous key
                uniqueLines.add("")
                previousLineKey = null
            }
            continue // Don't compare empty lines for duplication
        }


        val currentLineKey = createLineContentKey(trimmedLine)

        // Skip if the current line content key is essentially the same as the previous NON-EMPTY line
        if (currentLineKey == previousLineKey) {
            Log.d("MarkdownParse", "Skipping likely duplicate line: Original=\"$trimmedLine\", Key=\"$currentLineKey\"")
            continue
        }

        // Add the original line (with markdown) to our list and update the previous key
        uniqueLines.add(line) // Add the original line to preserve original markdown symbols/spacing before cleaning
        previousLineKey = currentLineKey // Set the key of the line we just added
    }
    val cleanedText = uniqueLines.joinToString("\n")
    // --- End: Pre-processing --- //

    Log.d("MarkdownParse", "Cleaned Text for Parsing:\n$cleanedText")

    // --- STANDARD MARKDOWN PARSING (using the cleanedText) ---
    return buildAnnotatedString {
        // Regex for bold (**text**), italic (*text* or _text_), headings (## text), list items (* text, - text, • text)
        val boldRegex = """\*\*(.*?)\*\*""".toRegex()
        // More specific italic regex to avoid matching stars inside bold ** like **like this*** (incorrect markdown)
        val italicStarRegex = Regex("""(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)""")
        val italicUnderscoreRegex = Regex("""(?<!_)\_(?!_)(.*?)(?<!\_)\_(?!\_)""")
        val headingRegex = """^##\s+(.*)""".toRegex(RegexOption.MULTILINE)
        val listItemRegex = """^\s*[*\-•]\s+(.+?)""".toRegex(RegexOption.MULTILINE) // Non-greedy + capture till end of intended item

        // Combine all potential matches
        val allMatches = (boldRegex.findAll(cleanedText).map { it to "bold" } +
                // Include refined italic matches
                italicStarRegex.findAll(cleanedText).map { it to "italic" } +
                italicUnderscoreRegex.findAll(cleanedText).map { it to "italic" } +
                headingRegex.findAll(cleanedText).map { it to "heading" } +
                listItemRegex.findAll(cleanedText).map { it to "list" })
            .sortedBy { it.first.range.first } // Process matches in order of appearance

        var currentIndex = 0
        for ((matchResult, type) in allMatches) {
            val range = matchResult.range
            // Append text before the current match
            if (range.first > currentIndex) {
                append(cleanedText.substring(currentIndex, range.first))
            }

            // Get the content *inside* the markdown symbols (or after for headings/lists)
            // Trim the content captured within markdown symbols
            val content = matchResult.groupValues[1].trim()


            // Apply style and append content
            when (type) {
                "bold" -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(content)
                    }
                }
                "italic" -> {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }
                "heading" -> {
                    // Ensure heading is on its own line(s)
                    if (currentIndex > 0 && !cleanedText.substring(0, range.first).endsWith("\n")) append("\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)) {
                        append(content)
                    }
                    // Add newlines after heading
                    if (range.last + 1 < cleanedText.length && cleanedText[range.last + 1] != '\n') append("\n")
                    append("\n") // Ensure at least one newline after a heading
                }
                "list" -> {
                    // Ensure list item starts on a new line
                    if (currentIndex > 0 && !cleanedText.substring(0, range.first).endsWith("\n")) append("\n")
                    append("  • ") // Use a consistent bullet point with indentation
                    append(content)
                    // Ensure list item ends with a newline
                    if (range.last + 1 < cleanedText.length && cleanedText[range.last + 1] != '\n') append("\n")
                    append("\n") // Ensure at least one newline after a list item
                }
            }
            // Move index past the processed match (including markdown symbols)
            currentIndex = range.last + 1
        }

        // Append any remaining text after the last match
        if (currentIndex < cleanedText.length) {
            append(cleanedText.substring(currentIndex))
        }
    }.trim() as AnnotatedString // <-- CAST TO AnnotatedString after trimming
    // We cast because .trim() on CharSequence returns CharSequence, but we know buildAnnotatedString
    // creates an AnnotatedString, and trimming it should result in another AnnotatedString.
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
                verticalAlignment = Alignment.Top // Align to top for multi-line text
            ) {
                // Parse markdown and display AnnotatedString
                // parseMarkdownToAnnotatedString now returns AnnotatedString
                val annotatedText: AnnotatedString = parseMarkdownToAnnotatedString(message.text)

                Text(
                    text = annotatedText, // Display the parsed AnnotatedString
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f, fill = false) // Allow text to wrap naturally
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