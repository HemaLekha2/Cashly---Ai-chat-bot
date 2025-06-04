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

class OpenAIHelper {
    // IMPORTANT: Replace with your actual Google AI Studio API key.
    // Store your API key securely (e.g., in local.properties) and DO NOT hardcode it here in production!
    private val apiKey = "Replace with your actual Google AI Studio API key"

    // Using gemini-1.5-flash-latest as it's cost-effective and fast.
    // You can change this to other models like "gemini-pro".
    private val modelName = "gemini-1.5-flash-latest"
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    suspend fun getChatbotReply(prompt: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        // Construct the request body according to Gemini API specification
        val contentsArray = JSONArray().apply {
            // Add system instruction as the first part of the first content object (user role)
            put(JSONObject().apply {
                put("role", "user") // Gemini often uses user/model roles
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", "Only give financial advice for the user input on savings.")
                }))
            })
            // Add the actual user prompt as the first part of the second content object
            put(JSONObject().apply {
                put("role", "user") // Followed by the user's actual prompt
                put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
            })
        }

        val reqBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            // Optional: Add generationConfig if needed (e.g., maxOutputTokens, temperature)
            // put("generationConfig", JSONObject().apply {
            //     put("maxOutputTokens", 150)
            // })
        }
        val reqBody = reqBodyJson.toString()

        Log.d("GeminiAPI", "Request Body: $reqBody")

        val body = reqBody.toRequestBody("application/json".toMediaTypeOrNull())

        // Build the request: API key is added as a query parameter
        val request = Request.Builder()
            .url("$endpoint?key=$apiKey") // API key as query parameter
            .post(body)
            .addHeader("Content-Type", "application/json") // Content-Type header is still needed
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string() ?: ""
            Log.d("GeminiAPI", "RAW RESPONSE: $raw")
            Log.d("GeminiAPI", "Response Code: ${response.code}")

            if (!response.isSuccessful) {
                // Try to parse error message from Gemini response
                try {
                    val errorJson = JSONObject(raw).optJSONObject("error")
                    val errorMessage = errorJson?.optString("message", "Unknown API error")
                    return@use "API error: ${response.code} - $errorMessage"
                } catch (e: Exception) {
                    return@use "API error: ${response.code} ${response.message}"
                }
            }

            // Parse the successful response
            return@use try {
                val json = JSONObject(raw)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        parts.getJSONObject(0).optString("text", "No content")?.trim()
                            ?: "No text part found in response."
                    } else {
                        "No parts found in response content."
                    }
                } else if (json.has("error")) { // Check for error object even in successful HTTP status (rare)
                    "API Error: " + json.getJSONObject("error").optString("message")
                } else {
                    "Sorry, I couldn't generate a reply. No candidates found."
                }
            } catch (e: Exception) {
                Log.e("GeminiAPI", "Parsing error", e)
                "Parsing error: ${e.localizedMessage}. Raw: $raw"
            }
        }
    }
}