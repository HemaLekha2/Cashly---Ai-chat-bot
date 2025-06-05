package com.example.smartspendchatbot.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAiHelper @Inject constructor() {

    // IMPORTANT: Hardcoded API Key - Replace with your actual key!
    // Storing API keys directly in code is a security risk.
    private val apiKey = "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE"

    private val modelName = "gemini-1.5-flash-latest"
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    // *** ENHANCED System instruction for detailed financial advice ***
    private val financialSystemInstruction = """
You are SmartSpend Bot, an expert financial advisor specializing in personal budgeting and expense optimization. Your primary goal is to provide highly detailed, actionable, and personalized financial guidance.

**Core Directives:**
1.  **Deep Analysis:** Meticulously analyze the provided monthly income and recent expenses. Calculate key ratios (e.g., expense categories as % of income). Identify spending patterns, potential overspending areas, and opportunities for savings.
2.  **Needs vs. Wants:** Rigorously categorize provided expenses into essential Needs (housing, basic food, utilities, essential transport, debt minimums) and discretionary Wants (entertainment, dining out, subscriptions, non-essential shopping). Be explicit about this categorization in your analysis.
3.  **Personalized Budget Plan:** If requested, create a detailed monthly budget plan based on the 50/30/20 rule (50% Needs, 30% Wants, 20% Savings/Debt Repayment) but *adjust percentages* based on the user's specific income and expense data to make it realistic. Clearly outline suggested spending amounts for major categories.
4.  **Actionable Optimization Strategies:** Provide specific, concrete, and prioritized recommendations for optimizing expenses and increasing savings. Quantify potential savings where possible (e.g., "Reducing dining out by X could save Y per month"). Offer practical tips (e.g., meal prepping, subscription audits, negotiating bills).
5.  **Clarity and Structure:** Present your response in a clear, organized manner using Markdown. Use headings (like `## Analysis`, `## Budget Plan`, `## Recommendations`), bullet points (`*` or `-`), and bold text (`**text**`) for emphasis and readability.
6.  **Proactive Guidance:** If the provided data is insufficient for a full analysis (e.g., too few expenses), explain what additional information is needed (e.g., "To create a full plan, please provide your expenses for the entire month.") and suggest how the user can track and provide it.
7.  **Tone:** Maintain a professional, encouraging, and supportive tone. Avoid generic advice; tailor it to the user's context.
"""

    // *** STRICT System instruction for basic/off-topic chat ***
    private val basicChatSystemInstruction = """
You are SmartSpend Bot, a financial assistant focused *exclusively* on personal finance, budgeting, expense tracking, and saving strategies.

**Your ONLY function is to discuss financial matters.**

*   **If the user asks a question directly related to their budget, expenses, income, savings goals, or asks for a financial plan/analysis, use the main financial analysis instructions.**
*   **If the user asks a question NOT related to personal finance (e.g., general knowledge, trivia, opinions, unrelated topics like animals or weather):**
    *   **Politely REFUSE to answer.**
    *   **State clearly that you can only discuss financial topics.**
    *   **Guide the user back to financial topics.**

**Example Refusals:**
*   "My expertise is in personal finance. I can help with budgeting or analyzing your spending, but I can't answer questions about [off-topic subject]."
*   "I can only assist with financial matters like budget planning and expense tracking. Can I help you create a budget plan or analyze your recent spending?"
*   "That's outside my area of focus, which is helping you manage your finances. Do you have any questions about your budget or savings?"

**DO NOT engage in off-topic conversation.**
"""

    // Function for detailed financial advice
    suspend fun getFinancialAdvice(financialContext: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE") {
            return@withContext "API Key not configured. Please paste your key into GoogleAiHelper.kt"
        }
        Log.d("GoogleAiHelper", "Using FINANCIAL system instruction.")
        val prompt = "User's financial context:\n$financialContext"
        return@withContext generateContent(prompt, financialSystemInstruction)
    }

    // Function for basic/off-topic chat replies
    suspend fun getChatbotReply(userPrompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE") {
            return@withContext "API Key not configured. Please paste your key into GoogleAiHelper.kt"
        }
        Log.d("GoogleAiHelper", "Using BASIC/OFF-TOPIC system instruction.")
        // This function now uses the strict basicChatSystemInstruction
        return@withContext generateContent(userPrompt, basicChatSystemInstruction)
    }

    // Internal function to call the Gemini API
    private suspend fun generateContent(userText: String, systemInstruction: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        // Ensure the API key is valid before making the call
        if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE") {
            Log.e("GoogleAiHelper", "API Key is missing or still the placeholder.")
            return@withContext "Error: API Key is not configured correctly in GoogleAiHelper.kt."
        }

        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().apply { put("text", systemInstruction) }))
            })
            put(JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(JSONObject().apply { put("text", "Okay, I understand my role and instructions.") })) // Adjusted model confirmation
            })
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().apply { put("text", userText) }))
            })
        }

        val reqBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6) // Slightly lower temperature for more factual financial advice
                put("maxOutputTokens", 600) // Allow slightly longer responses for detailed advice
            })
        }
        val reqBody = reqBodyJson.toString()

        Log.d("GoogleAiHelper", "Request Body: $reqBody")

        val body = reqBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$endpoint?key=$apiKey")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string() ?: ""
                Log.d("GoogleAiHelper", "RAW RESPONSE: $raw")
                Log.d("GoogleAiHelper", "Response Code: ${response.code}")

                if (!response.isSuccessful) {
                    return@use parseErrorResponse(raw, response.code, response.message)
                }

                return@use parseSuccessResponse(raw)
            }
        } catch (e: Exception) {
            Log.e("GoogleAiHelper", "Network or API call error", e)
            return@withContext "Network error: ${e.message}"
        }
    }

    private fun parseSuccessResponse(raw: String): String {
        return try {
            val json = JSONObject(raw)
            val promptFeedback = json.optJSONObject("promptFeedback")
            if (promptFeedback != null) {
                val blockReason = promptFeedback.optString("blockReason", null)
                if (blockReason != null) {
                    Log.w("GoogleAiHelper", "Response blocked by API. Reason: $blockReason")
                    return "My response was blocked. This might be due to safety settings. Please try rephrasing your request."
                }
            }

            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val finishReason = candidate.optString("finishReason", "")
                if (finishReason != "STOP" && finishReason != "MAX_TOKENS") { // Allow MAX_TOKENS as a valid finish
                    Log.w("GoogleAiHelper", "Unexpected finish reason: $finishReason")
                }

                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    parts.getJSONObject(0).optString("text", "").trim()
                } else {
                    Log.e("GoogleAiHelper", "No text parts found in candidate content.")
                    "Sorry, I received an empty response content."
                }
            } else {
                val error = json.optJSONObject("error")
                if (error != null) {
                    Log.e("GoogleAiHelper", "API returned an error object: ${error.toString()}")
                    "API Error: ${error.optString("message", "Unknown error from API")}"
                } else {
                    Log.e("GoogleAiHelper", "No candidates found and no error object.")
                    "Sorry, I couldn't generate a reply. No candidates found."
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleAiHelper", "JSON Parsing error", e)
            "Error parsing response: ${e.localizedMessage}. Raw: $raw"
        }
    }

    private fun parseErrorResponse(raw: String, code: Int, message: String): String {
        return try {
            val errorJson = JSONObject(raw).optJSONObject("error")
            val errorMessage = errorJson?.optString("message", "Unknown API error")
            Log.e("GoogleAiHelper", "API Error $code: $errorMessage. Raw: $raw")
            "API error ($code): $errorMessage"
        } catch (e: Exception) {
            Log.e("GoogleAiHelper", "Failed to parse error response. Code: $code, Message: $message. Raw: $raw", e)
            "API error: $code $message."
        }
    }
}
