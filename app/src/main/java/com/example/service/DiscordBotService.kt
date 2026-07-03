package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.PyBotApplication
import com.example.data.model.BotConfig
import com.example.data.repository.GeminiService
import com.example.data.repository.PyBotRepository
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class DiscordBotService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: PyBotRepository
    private val geminiService = GeminiService()
    private val httpClient = OkHttpClient()

    private var gatewayClient: DiscordGatewayClient? = null
    private var botToken: String = ""
    private var scriptCode: String = ""
    
    private var messagesProcessed = 0
    private var activeServers = 0
    private var botName = "Python Bot"
    private var startTimeMillis = 0L

    companion object {
        private const val TAG = "DiscordBotService"
        private const val CHANNEL_ID = "PyBotServiceChannel"
        private const val NOTIFICATION_ID = 4242
        
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        repository = (application as PyBotApplication).repository
        startTimeMillis = System.currentTimeMillis()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopBot()
            return START_NOT_STICKY
        }

        botToken = intent?.getStringExtra("TOKEN") ?: ""
        scriptCode = intent?.getStringExtra("SCRIPT") ?: ""

        if (botToken.isEmpty() || scriptCode.isEmpty()) {
            repository.addLog("[System] Failed to start bot: Missing script or Token.")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground Notification
        startForeground(NOTIFICATION_ID, buildNotification("Booting..."))

        // Start Bot
        startBot()

        return START_STICKY
    }

    private fun startBot() {
        serviceScope.launch {
            repository.addLog("[Python Runner] Initializing sandbox environment...")
            delay(500)
            repository.addLog("[Python Runner] Virtual Python runtime starting...")
            delay(500)
            repository.addLog("[Python Runner] Python packages loaded: [discord.py, requests, asyncio]")
            delay(300)
            repository.addLog("[Python Runner] Launching process: python3 main.py")
            delay(600)

            // Setup WebSocket Gateway Client
            gatewayClient = DiscordGatewayClient(
                token = botToken,
                scriptCode = scriptCode,
                repository = repository,
                onBotReady = { name, guildsCount ->
                    botName = name
                    activeServers = guildsCount
                    updateBotConfigInDb("Online")
                    updateNotification("Online - Active on $guildsCount server(s)")
                },
                onMessageReceived = { channelId, author, messageId, content ->
                    handleDiscordMessage(channelId, author, content)
                },
                onStatusChanged = { status ->
                    updateBotConfigInDb(status)
                    updateNotification(status)
                }
            )

            gatewayClient?.connect()
        }
    }

    private fun handleDiscordMessage(channelId: String, author: String, content: String) {
        serviceScope.launch {
            repository.addLog("[Discord Gateway] Message from $author in channel $channelId: \"$content\"")
            
            // Increment statistics
            messagesProcessed++
            updateBotConfigInDb("Online")

            // Evaluate in Python Virtual Sandbox via Gemini
            repository.addLog("[Python Interpreter] Evaluating script hooks for message: \"$content\"...")
            
            val (logs, responseText) = geminiService.evaluateDiscordMessage(
                script = scriptCode,
                channelId = channelId,
                author = author,
                content = content
            )

            // Feed logs back to the console
            logs.forEach { logLine ->
                repository.addLog(logLine)
            }

            // Dispatch message back to Discord REST API if one is produced
            if (responseText != null) {
                repository.addLog("[Python Runtime] Sending reply payload to Discord: \"$responseText\"")
                sendDiscordMessage(channelId, responseText)
            } else {
                repository.addLog("[Python Runtime] Process finished (no matching event rules or command fired).")
            }
        }
    }

    private fun sendDiscordMessage(channelId: String, text: String) {
        serviceScope.launch {
            val url = "https://discord.com/api/v10/channels/$channelId/messages"
            
            val json = JSONObject()
            json.put("content", text)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bot $botToken")
                .post(requestBody)
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        repository.addLog("[API Success] Message successfully delivered to Discord.")
                    } else {
                        val errorBody = response.body?.string() ?: ""
                        repository.addLog("[API Error] Failed to send message: Code ${response.code} - $errorBody")
                        Log.e(TAG, "API Error: ${response.code} $errorBody")
                    }
                }
            } catch (e: IOException) {
                repository.addLog("[Network Error] Exception calling Discord API: ${e.message}")
                Log.e(TAG, "Network exception sending message", e)
            }
        }
    }

    private fun updateBotConfigInDb(status: String) {
        serviceScope.launch {
            val currentConfig = repository.getBotConfig() ?: BotConfig()
            repository.updateBotConfig(
                currentConfig.copy(
                    status = status,
                    botName = botName,
                    activeServers = activeServers,
                    messagesProcessed = messagesProcessed,
                    uptimeMillis = System.currentTimeMillis() - startTimeMillis
                )
            )
        }
    }

    private fun stopBot() {
        repository.addLog("[System] Stopping bot execution process...")
        gatewayClient?.disconnect()
        gatewayClient = null
        updateBotConfigInDb("Offline")
        stopSelf()
    }

    private fun updateNotification(status: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun buildNotification(status: String): Notification {
        val stopIntent = Intent(this, DiscordBotService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PyBot Bot Running")
            .setContentText("Status: $status | Messages: $messagesProcessed")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Bot", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "PyBot Bot Runner Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        Log.d(TAG, "Foreground Service Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
