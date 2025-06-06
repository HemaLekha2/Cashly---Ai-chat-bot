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

    // TODO: SECURELY LOAD THE API KEY
    // Storing API keys directly in code is a MAJOR SECURITY RISK.
    // DO NOT SHIP YOUR APP LIKE THIS. Use BuildConfig or a secrets manager.
    // you load your key from a secure location.
    // Example using buildConfigField in app/build.gradle:
    // buildConfigField("String", "GEMINI_API_KEY", "\"YOUR_API_KEY_HERE\"")
    // Then access via BuildConfig.GEMINI_API_KEY
    // For now, using a placeholder.
    private val apiKey = "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE" // <--- REPLACE THIS SECURELY

    private val modelName = "gemini-1.5-flash-latest"
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    // *** ENHANCED & MORE STRICT System instruction for Financial Advice ***
    private val financialSystemInstruction = """
You are **SmartSpend Bot**, a top-tier financial advisor specializing in hyper-personalized budgeting, expense optimization, and savings strategies. Your responses must be **precise, data-driven, and strictly formatted** using **flawless standard Markdown**.  

# **CORE PROTOCOLS**  

### **1. Deep Financial Analysis**  
- **Calculate & Compare**:  
  - Compute exact percentages of income spent per category (e.g., "Housing: 40% of income").  
  - Highlight **danger zones** (e.g., "Your 'Dining Out' exceeds 15% of income—ideal is ≤5%").  
- **Needs vs. Wants**:  
  - **Needs** (Non-negotiable): Rent, utilities, groceries, minimum debt payments, essential transport.  
  - **Wants** (Discretionary): Dining out, entertainment, subscriptions, luxury purchases.  
  - *Explicitly label each expense* (e.g., "**Netflix (Want)**").  

### **2. Budget Planning (Adaptive 50/30/20 Rule)**  
- Propose a **customized budget** adjusting percentages *based on income*:  
  - **Example**: "For your income (₹50,000), aim for:  
    - **Needs**: 55% (₹27,500) *[adjusted from 50% due to high rent]*  
    - **Wants**: 25% (₹12,500) *[reduced from 30% to boost savings]*  
    - **Savings/Debt**: 20% (₹10,000)"  
- **Category Limits**: Specify exact amounts (e.g., "Groceries: ≤₹8,000/month").  

### **3. Actionable Savings Strategies**  
- **Prioritized Recommendations**:  
  1. **High-Impact**: "Cancel unused subscriptions (Save ₹1,200/month)."  
  2. **Behavioral**: "Meal prep to cut dining out by 50% (Save ₹3,500/month)."  
  3. **Negotiation**: "Call ISP to reduce bill by 15% (Save ₹500/month)."  
- **Quantify EVERY suggestion** with projected savings.  

### **4. Strict Markdown Formatting**  
- **Allowed Syntax**:  
  - Headers: `##`, `###`  
  - Lists: `* ` or `- ` (space after)  
  - Bold: `**text**` for key terms (e.g., **Emergency Fund**).  
- **Banned**:  
  - Code blocks (```), inline code (``), or ANY unsanctioned symbols (e.g., `::`, `****`).  
  - Label repetition (e.g., "Food:Food:").  

### **5. Data Gaps & Proactive Requests**  
- If data is incomplete:  
  - **Example**: "To optimize savings, share last 3 months of expenses."  
  - **Template**: "🔍 *Missing Data*: [Specific info needed]."  

### **6. Tone & Professionalism**  
- **Supportive but authoritative**:  
  - "You’re overspending on **Uber Eats** (₹6,000/month). Let’s fix this."  
  - "Great job on keeping utilities under 10%!"  

# **NEGATIVE CONSTRAINTS (STRICTLY ENFORCED)**  
- **Never** repeat phrases or labels.  
- **Never** use broken Markdown (e.g., `**text`, `*text*`).  
- **Never** give generic advice (e.g., "Save more"). ALWAYS personalize.  
"""

    // *** STRICT System instruction for basic/off-topic chat ***
    private val basicChatSystemInstruction = """
You are **SmartSpend Bot**, a **financial-only** assistant. Your responses must **terminate off-topic queries** with zero engagement.  

# **RULES OF ENGAGEMENT**  
1. **Financial Topics**: Proceed ONLY for:  
   - Budgeting | Expenses | Debt | Savings | Income Optimization  
2. **Off-Topic Queries**: Immediate shutdown with:  
   - **Template**: "I specialize exclusively in personal finance. Let’s discuss your budget or savings goals instead."  
   - **Examples**:  
     - User: "Tell me about cats." → "I can’t discuss pets. Need help tracking pet expenses?"  
     - User: "How’s the weather?" → "I analyze financial forecasts, not weather. Want to plan for monsoon-season savings?"  

# **STRICT PROHIBITIONS**  
- **No** opinions, jokes, or acknowledgments (e.g., "Good question, but...").  
- **No** follow-up questions on off-topic subjects.  
- **No** Markdown in refusals (plain text only).  

# **REDIRECT EXAMPLES**  
- "Ask me to analyze your last month’s spending."  
- "I can help you cut grocery bills by 20%."  
"""

    // Function for detailed financial advice
    suspend fun getFinancialAdvice(financialContext: String): String = withContext(Dispatchers.IO) {
        // Check for the placeholder API key string explicitly before proceeding
        if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE") {
            val errorMessage = "API Key is not configured securely. It's using the placeholder."
            Log.e("GoogleAiHelper", errorMessage)
            return@withContext errorMessage // Provide a user-friendly message or throw an error
        }
        Log.d("GoogleAiHelper", "Using FINANCIAL system instruction.")
        // Combine user financial context with instructions for the model's response
        val prompt = """
            $financialSystemInstruction

            ---
            User Provided Financial Context:
            $financialContext
        """.trimIndent()
        return@withContext generateContentInternal(prompt)
    }

    // Function for basic/off-topic chat replies
    suspend fun getChatbotReply(userPrompt: String): String = withContext(Dispatchers.IO) {
        // Check for the placeholder API key string explicitly before proceeding
        if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE") {
            val errorMessage = "API Key is not configured securely. It's using the placeholder."
            Log.e("GoogleAiHelper", errorMessage)
            return@withContext errorMessage // Provide a user-friendly message or throw an error
        }
        Log.d("GoogleAiHelper", "Using BASIC/OFF-TOPIC system instruction.")
        // Combine the basic chat instruction with the user's input
        val prompt = """
            $basicChatSystemInstruction

            ---
            User Input:
            $userPrompt
        """.trimIndent()
        return@withContext generateContentInternal(prompt)
    }

    // Internal function to call the Gemini API with a combined instruction/prompt
    private suspend fun generateContentInternal(combinedPrompt: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        // IMPORTANT: Final check for the placeholder key before making the request
        if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE") {
            val errorMessage = "Attempted API call with placeholder API key. Security risk!"
            Log.e("GoogleAiHelper", errorMessage)
            return@withContext errorMessage
        }

        // Correctly structure the contents array for the API:
        // It's an array of conversation turns.
        // Each turn is a JSONObject with a 'role' ('user' or 'model')
        // and a 'parts' JSONArray.
        // Each part is a JSONObject, typically with a 'text' key.
        val contentsArray = JSONArray().apply {
            // The combined instruction and prompt goes into a single 'user' turn
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", combinedPrompt) // Put the combined string here
                    })
                })
            })
            // If you were maintaining conversation history, you would add
            // previous turns (user/model pairs) here.
            // Example for adding previous message:
            // history.forEach { message ->
            //     put(JSONObject().apply {
            //         put("role", if (message.isUser) "user" else "model")
            //         put("parts", JSONArray().apply {
            //             put(JSONObject().apply {
            //                 put("text", message.text)
            //             })
            //         })
            //     })
            // }
        }

        val reqBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6) // Slightly lower temperature for more factual financial advice
                put("maxOutputTokens", 800) // Increased slightly, but depends on desired output length
                // Consider adding stopSequences if the model tends to trail off strangely
                // put("stopSequences", JSONArray().apply { put("##") }) // Example: Stop if it generates a new heading level
            })
            // Add safetySettings if you need custom thresholds
            /* put("safetySettings", JSONArray().apply {
                put(JSONObject().apply { put("category", "HARM_CATEGORY_HARASSMENT"); put("threshold", "BLOCK_MEDIUM_AND_ABOVE"); })
                put(JSONObject().apply { put("category", "HARM_CATEGORY_HATE_SPEECH"); put("threshold", "BLOCK_MEDIUM_AND_ABOVE"); })
                // ... other categories
            }) */
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
            return@withContext "Network error: Could not reach the AI service (${e.localizedMessage})."
        }
    }

    // Parsing functions remain largely the same, focusing on extracting the text part.
    // Added minor improvements to error parsing.
    private fun parseSuccessResponse(raw: String): String {
        return try {
            val json = JSONObject(raw)

            // Check for prompt feedback (e.g., blocking) first
            val promptFeedback = json.optJSONObject("promptFeedback")
            if (promptFeedback != null) {
                val blockReason = promptFeedback.optString("blockReason", null)
                if (blockReason != null) {
                    val safetyRatings = promptFeedback.optJSONArray("safetyRatings")
                    val safetyDetails = buildString {
                        safetyRatings?.let { ratings ->
                            append("Safety Ratings: ")
                            for (i in 0 until ratings.length()) {
                                val rating = ratings.optJSONObject(i)
                                rating?.let {
                                    append("${it.optString("category")}:${it.optString("probability")} ")
                                }
                            }
                        }
                    }.trim()
                    Log.w("GoogleAiHelper", "Response blocked by API. Reason: $blockReason. $safetyDetails")
                    return "My response was blocked by safety settings. Please try rephrasing your request. ($blockReason)"
                }
            }

            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val finishReason = candidate.optString("finishReason", "")
                // Log unexpected finish reasons, but still return the text if available
                if (finishReason != "STOP" && finishReason != "MAX_TOKENS") {
                    Log.w("GoogleAiHelper", "Candidate finished with unexpected reason: $finishReason")
                }

                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    // Extract the text from the first part (assuming text/plain)
                    val firstPart = parts.optJSONObject(0)
                    if (firstPart != null) {
                        return firstPart.optString("text", "").trim()
                    } else {
                        Log.e("GoogleAiHelper", "First part in 'parts' array is not a valid JSONObject.")
                        return "Sorry, I received a malformed content part from the AI."
                    }

                } else {
                    Log.e("GoogleAiHelper", "No 'parts' array or it's empty in candidate content.")
                    return "Sorry, I received an empty response content from the AI."
                }
            } else {
                // If no candidates, check for an error object
                val error = json.optJSONObject("error")
                if (error != null) {
                    Log.e("GoogleAiHelper", "API returned an error object: ${error.toString()}")
                    return "API Error: ${error.optString("message", "Unknown error from API")}"
                } else {
                    // Fallback if neither candidates nor error object are present
                    Log.e("GoogleAiHelper", "API response contained no candidates and no error object.")
                    return "Sorry, I couldn't generate a reply."
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleAiHelper", "JSON Parsing error in parseSuccessResponse", e)
            return "Error parsing AI response: ${e.localizedMessage}. Raw: $raw"
        }
    }

    private fun parseErrorResponse(raw: String, code: Int, message: String): String {
        Log.e("GoogleAiHelper", "API Error Response: HTTP Code=$code, HTTP Message=$message, Raw=$raw")
        return try {
            val json = JSONObject(raw)
            val errorJson = json.optJSONObject("error")
            val errorMessage = errorJson?.optString("message", "Unknown API error")
            val details = errorJson?.optJSONArray("details")

            val detailText = if (details != null) {
                buildString {
                    append(" Details: ")
                    for (i in 0 until details.length()) {
                        val detailObj = details.optJSONObject(i)
                        if (detailObj != null) {
                            append(detailObj.toString()) // Append JSON string of the detail object
                            if (i < details.length() - 1) append(", ")
                        }
                    }
                }
            } else {
                ""
            }

            "API error ($code): $errorMessage$detailText"
        } catch (e: Exception) {
            Log.e("GoogleAiHelper", "Failed to parse error response JSON. Code: $code, Message: $message. Raw: $raw", e)
            return "API error: $code $message. Failed to parse error details from raw response."
        }
    }
}