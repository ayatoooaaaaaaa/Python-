package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiService"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun generateCode(prompt: String, currentScript: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "# Error: Gemini API key is missing. Please set it in AI Studio Secrets."
        }

        val fullPrompt = """
            You are an expert Discord.py developer. 
            The user wants to generate or modify a Python Discord bot script.
            
            Current script:
            ```python
            $currentScript
            ```
            
            Instruction:
            $prompt
            
            Please provide the FULL revised python script. Do not output conversational filler. Just the clean, valid Python code enclosed in a ```python block.
        """.trimIndent()

        try {
            val responseText = callGemini(apiKey, fullPrompt, false)
            extractCodeBlock(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating code", e)
            "# Error: Failed to generate code: ${e.localizedMessage}"
        }
    }

    suspend fun fixCode(error: String, currentScript: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "# Error: Gemini API key is missing. Please set it in AI Studio Secrets."
        }

        val fullPrompt = """
            You are an expert Python debugger. 
            The following Python script has crashed or thrown an error during execution.
            
            Current script:
            ```python
            $currentScript
            ```
            
            Runtime Error:
            $error
            
            Please fix the error. Return the COMPLETE corrected Python script. Do not include any conversational preamble. Just the valid corrected python code in a ```python block.
        """.trimIndent()

        try {
            val responseText = callGemini(apiKey, fullPrompt, false)
            extractCodeBlock(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing code", e)
            "# Error: Failed to fix code: ${e.localizedMessage}"
        }
    }

    suspend fun evaluateDiscordMessage(
        script: String,
        channelId: String,
        author: String,
        content: String
    ): Pair<List<String>, String?> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Pair(
                listOf("[Warning] Gemini API Key is missing. Using offline standard responder."),
                getOfflineResponse(content)
            )
        }

        val systemInstruction = """
            You are an embedded Python 3 execution runtime sandbox that executes discord.py bot scripts.
            Your task is to mentally execute the provided script in response to a Discord event.
            
            You must return a JSON object with EXACTLY this structure:
            {
              "logs": ["list of stdout print lines printed during event execution"],
              "response": "the text content of the message sent back to the channel, or null if no command was matched or no reply was sent"
            }
        """.trimIndent()

        val prompt = """
            Python Script:
            ```python
            $script
            ```
            
            Discord Event:
            - Type: MESSAGE_CREATE
            - Channel ID: $channelId
            - Author Name: $author
            - Content: $content
            
            Please execute the matching event handler or command. Make sure to generate realistic terminal prints (like loading command_prefix, login triggers, etc.) and the appropriate response.
            Return ONLY the raw JSON object conforming to the schema. No markdown formatting.
        """.trimIndent()

        try {
            val rawJson = callGemini(apiKey, prompt, true, systemInstruction)
            val cleanJson = cleanJsonString(rawJson)
            val jsonObject = JSONObject(cleanJson)
            
            val logsArray = jsonObject.optJSONArray("logs")
            val logsList = mutableListOf<String>()
            if (logsArray != null) {
                for (i in 0 until logsArray.length()) {
                    logsList.add(logsArray.getString(i))
                }
            } else {
                logsList.add("[Bot Core] Executed successfully.")
            }
            
            val responseText = jsonObject.optString("response", null)
            val realResponse = if (responseText == "null" || responseText.isNullOrEmpty()) null else responseText
            
            Pair(logsList, realResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing script simulation with Gemini", e)
            Pair(
                listOf("[Bot Error] Python syntax or runtime exception: ${e.localizedMessage}"),
                null
            )
        }
    }

    private fun getOfflineResponse(content: String): String? {
        val cmd = content.trim()
        return when {
            cmd.startsWith("!ping") -> "Pong! 🏓 (Offline mode active)"
            cmd.startsWith("!hello") -> "Hello! I am running in Offline Mode on Android. Configure your Gemini API key for full AI-driven Python execution! 🤖"
            cmd.startsWith("!info") -> "PyBot Runner: Running in Offline backup. Please set GEMINI_API_KEY to run complex Discord.py bot actions."
            else -> null
        }
    }

    private fun extractCodeBlock(text: String): String {
        val startMarker = "```python"
        val endMarker = "```"
        if (text.contains(startMarker)) {
            val start = text.indexOf(startMarker) + startMarker.length
            val end = text.indexOf(endMarker, start)
            if (end != -1) {
                return text.substring(start, end).trim()
            }
        }
        if (text.contains("```")) {
            val start = text.indexOf("```") + 3
            val end = text.indexOf("```", start)
            if (end != -1) {
                return text.substring(start, end).trim()
            }
        }
        return text.trim()
    }

    private fun cleanJsonString(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }
        return clean
    }

    private suspend fun callGemini(
        apiKey: String,
        prompt: String,
        isJson: Boolean,
        systemInstruction: String? = null
    ): String {
        val url = "$BASE_URL?key=$apiKey"
        
        val requestBodyJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObject = JSONObject()
        val partsArray = JSONArray()
        val partObject = JSONObject()
        
        partObject.put("text", prompt)
        partsArray.put(partObject)
        contentObject.put("parts", partsArray)
        contentsArray.put(contentObject)
        requestBodyJson.put("contents", contentsArray)

        // Configuration
        val config = JSONObject()
        if (isJson) {
            config.put("responseMimeType", "application/json")
        }
        requestBodyJson.put("generationConfig", config)

        // System Instruction
        if (!systemInstruction.isNullOrEmpty()) {
            val systemObj = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", systemInstruction)
            sysParts.put(sysPart)
            systemObj.put("parts", sysParts)
            requestBodyJson.put("systemInstruction", systemObj)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code} ${response.message}")
            }
            val resText = response.body?.string() ?: throw Exception("Empty response from Gemini")
            val jsonRes = JSONObject(resText)
            
            val candidates = jsonRes.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val resContent = firstCandidate.getJSONObject("content")
            val parts = resContent.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }
}
