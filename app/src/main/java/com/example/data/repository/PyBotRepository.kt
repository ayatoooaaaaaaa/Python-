package com.example.data.repository

import android.content.Context
import com.example.data.dao.BotConfigDao
import com.example.data.dao.ModuleDao
import com.example.data.dao.ScriptDao
import com.example.data.model.BotConfig
import com.example.data.model.ModulePackage
import com.example.data.model.Script
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class PyBotRepository(
    private val scriptDao: ScriptDao,
    private val moduleDao: ModuleDao,
    private val botConfigDao: BotConfigDao,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient()
    // Console logs stored in-memory
    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    // Expose flows from database
    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts()
    val allModules: Flow<List<ModulePackage>> = moduleDao.getAllModules()
    val botConfig: Flow<BotConfig?> = botConfigDao.getBotConfigFlow()

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun addLog(message: String) {
        val timestamp = timeFormatter.format(Date())
        val formatted = "[$timestamp] $message"
        _consoleLogs.update { current ->
            val updated = current.toMutableList()
            updated.add(formatted)
            // Limit to last 300 entries to avoid memory bloat
            if (updated.size > 300) {
                updated.removeAt(0)
            }
            updated
        }
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
        addLog("Console logs cleared.")
    }

    // Scripts management
    suspend fun getScriptById(id: Int): Script? = scriptDao.getScriptById(id)
    suspend fun insertScript(script: Script): Long = scriptDao.insertScript(script)
    suspend fun updateScript(script: Script) = scriptDao.updateScript(script)
    suspend fun deleteScript(script: Script) = scriptDao.deleteScript(script)

    // Config management
    suspend fun getBotConfig(): BotConfig? = botConfigDao.getBotConfig()
    suspend fun updateBotConfig(config: BotConfig) = botConfigDao.insertOrUpdate(config)

    // Module Manager and installer simulation
    fun installModule(moduleName: String) {
        scope.launch(Dispatchers.IO) {
            val module = moduleDao.getModuleByName(moduleName) ?: return@launch
            addLog("pip: Checking dependencies for $moduleName...")
            delay(400)
            addLog("pip: Downloading $moduleName (${module.size})...")

            // Simulate progress download
            for (progress in 1..10) {
                delay(250)
                val pct = progress * 10f / 100f
                moduleDao.updateDownloadState(moduleName, isInstalled = false, progress = pct)
                addLog("pip: Downloading $moduleName... ${progress * 10}%")
            }

            addLog("pip: Preparing metadata for $moduleName...")
            delay(300)
            addLog("pip: Installing wheel file for $moduleName...")
            delay(500)
            addLog("pip: Building wheels for collected packages: $moduleName...")
            delay(400)

            // Mark as installed
            moduleDao.updateDownloadState(moduleName, isInstalled = true, progress = 1.0f)
            addLog("pip: Successfully installed $moduleName-${module.version}")
        }
    }

    fun uninstallModule(moduleName: String) {
        scope.launch(Dispatchers.IO) {
            addLog("pip: Uninstalling $moduleName...")
            delay(500)
            moduleDao.updateDownloadState(moduleName, isInstalled = false, progress = 0.0f)
            addLog("pip: Successfully uninstalled $moduleName")
        }
    }
}
