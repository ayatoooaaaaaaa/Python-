package com.example.service

import android.util.Log
import com.example.data.repository.PyBotRepository
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class DiscordGatewayClient(
    private val token: String,
    private val scriptCode: String,
    private val repository: PyBotRepository,
    private val onBotReady: (String, Int) -> Unit,
    private val onMessageReceived: (String, String, String, String) -> Unit, // channelId, author, messageId, content
    private val onStatusChanged: (String) -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var lastSequence: Int? = null
    private var sessionId: String? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isConnected = false

    companion object {
        private const val TAG = "DiscordGateway"
        private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
    }

    fun connect() {
        if (isConnected) return
        onStatusChanged("Connecting")
        repository.addLog("Gateway: Initializing connection to Discord...")

        val request = Request.Builder()
            .url(GATEWAY_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                repository.addLog("Gateway: Socket opened successfully. Waiting for Hello...")
                Log.d(TAG, "WebSocket Opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleGatewayMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                repository.addLog("Gateway: Closing socket ($code) - $reason")
                Log.d(TAG, "WebSocket Closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                cleanup()
                repository.addLog("Gateway: Offline. Connection closed.")
                onStatusChanged("Offline")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                cleanup()
                val errMsg = t.message ?: "Unknown socket failure"
                repository.addLog("Gateway Error: $errMsg")
                Log.e(TAG, "WebSocket Failure", t)
                onStatusChanged("Error")
            }
        })
    }

    fun disconnect() {
        if (!isConnected) return
        repository.addLog("Gateway: Shutting down connection...")
        webSocket?.close(1000, "User requested stop")
        cleanup()
    }

    private fun cleanup() {
        isConnected = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket = null
        lastSequence = null
    }

    private fun handleGatewayMessage(messageText: String) {
        try {
            val json = JSONObject(messageText)
            val op = json.getInt("op")
            
            if (json.has("s") && !json.isNull("s")) {
                lastSequence = json.getInt("s")
            }

            when (op) {
                10 -> { // Hello
                    val d = json.getJSONObject("d")
                    val heartbeatInterval = d.getLong("heartbeat_interval")
                    repository.addLog("Gateway: Handshake received. Heartbeat set to ${heartbeatInterval}ms.")
                    startHeartbeat(heartbeatInterval)
                    sendIdentify()
                }
                11 -> { // Heartbeat ACK
                    Log.d(TAG, "Heartbeat acknowledged by gateway")
                }
                0 -> { // Dispatch events
                    val t = json.getString("t")
                    val d = json.getJSONObject("d")
                    handleDispatchEvent(t, d)
                }
                else -> {
                    Log.d(TAG, "Unhandled Opcode: $op")
                }
            }
        } catch (e: Exception) {
            repository.addLog("Gateway Parse Error: ${e.message}")
            Log.e(TAG, "Failed to parse gateway message", e)
        }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                sendHeartbeat()
            }
        }
    }

    private fun sendHeartbeat() {
        if (!isConnected) return
        val payload = JSONObject()
        payload.put("op", 1)
        payload.put("d", if (lastSequence != null) lastSequence else JSONObject.NULL)
        webSocket?.send(payload.toString())
        Log.d(TAG, "Heartbeat sent")
    }

    private fun sendIdentify() {
        if (!isConnected) return
        repository.addLog("Gateway: Authenticaton... Sending Identify token payload.")
        
        val payload = JSONObject()
        payload.put("op", 2)
        
        val d = JSONObject()
        d.put("token", token)
        
        val properties = JSONObject()
        properties.put("os", "android")
        properties.put("browser", "pybot_runner")
        properties.put("device", "pybot_runner")
        d.put("properties", properties)
        
        // Intents: GUILDS (1) + GUILD_MESSAGES (512) + MESSAGE_CONTENT (32768) = 33281
        d.put("intents", 33281)
        
        payload.put("d", d)
        webSocket?.send(payload.toString())
    }

    private fun handleDispatchEvent(type: String, data: JSONObject) {
        scope.launch {
            try {
                when (type) {
                    "READY" -> {
                        sessionId = data.getString("session_id")
                        val user = data.getJSONObject("user")
                        val botUsername = user.getString("username")
                        val botDiscriminator = user.getString("discriminator")
                        val botFullName = "$botUsername#$botDiscriminator"
                        
                        val guilds = data.getJSONArray("guilds")
                        val activeGuildsCount = guilds.length()

                        repository.addLog("Gateway SUCCESS: Logged in as $botFullName!")
                        repository.addLog("Gateway: Connected to $activeGuildsCount Discord server(s).")
                        
                        onBotReady(botFullName, activeGuildsCount)
                        onStatusChanged("Online")
                    }
                    "MESSAGE_CREATE" -> {
                        val authorJson = data.getJSONObject("author")
                        val authorName = authorJson.getString("username")
                        val isBot = authorJson.optBoolean("bot", false)
                        
                        // Avoid infinite loops responding to itself
                        if (isBot) return@launch
                        
                        val channelId = data.getString("channel_id")
                        val messageId = data.getString("id")
                        val content = data.getString("content")

                        Log.d(TAG, "Message from $authorName: $content")
                        onMessageReceived(channelId, authorName, messageId, content)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling dispatch event $type", e)
            }
        }
    }
}
