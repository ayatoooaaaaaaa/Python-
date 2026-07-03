package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.PyBotApplication
import com.example.data.model.BotConfig
import com.example.data.model.ModulePackage
import com.example.data.model.Script
import com.example.data.repository.GeminiService
import com.example.data.repository.PyBotRepository
import com.example.service.DiscordBotService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PyBotViewModel(private val repository: PyBotRepository) : ViewModel() {
    private val geminiService = GeminiService()

    // Screen states
    val scripts = repository.allScripts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val modules = repository.allModules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val botConfig = repository.botConfig.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val consoleLogs = repository.consoleLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Editor State
    private val _selectedScript = MutableStateFlow<Script?>(null)
    val selectedScript: StateFlow<Script?> = _selectedScript.asStateFlow()

    private val _editorCode = MutableStateFlow("")
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()

    private val _editorName = MutableStateFlow("")
    val editorName: StateFlow<String> = _editorName.asStateFlow()

    private val _editorDescription = MutableStateFlow("")
    val editorDescription: StateFlow<String> = _editorDescription.asStateFlow()

    // Import Scanner results
    private val _missingDetectedModules = MutableStateFlow<List<ModulePackage>>(emptyList())
    val missingDetectedModules: StateFlow<List<ModulePackage>> = _missingDetectedModules.asStateFlow()

    // Loading / Processing State
    private val _aiOperationInProgress = MutableStateFlow(false)
    val aiOperationInProgress: StateFlow<Boolean> = _aiOperationInProgress.asStateFlow()

    init {
        // Collect scripts to load the initial selected script when database initializes
        viewModelScope.launch {
            combine(scripts, botConfig) { scriptList, config ->
                Pair(scriptList, config)
            }.collect { (scriptList, config) ->
                if (scriptList.isNotEmpty() && _selectedScript.value == null) {
                    val targetId = config?.selectedScriptId ?: scriptList.first().id
                    val initialScript = scriptList.find { it.id == targetId } ?: scriptList.first()
                    selectScript(initialScript)
                }
            }
        }

        // Run automated static analyzer for imports on code edits
        viewModelScope.launch {
            combine(editorCode, modules) { code, allMods ->
                Pair(code, allMods)
            }.collect { (code, allMods) ->
                if (code.isNotEmpty() && allMods.isNotEmpty()) {
                    _missingDetectedModules.value = scanCodeForImports(code, allMods)
                }
            }
        }
    }

    fun selectScript(script: Script) {
        _selectedScript.value = script
        _editorCode.value = script.content
        _editorName.value = script.name
        _editorDescription.value = script.description
        
        // Update database configuration selection row
        viewModelScope.launch(Dispatchers.IO) {
            val currentConfig = repository.getBotConfig() ?: BotConfig()
            repository.updateBotConfig(currentConfig.copy(selectedScriptId = script.id))
            repository.addLog("Script [${script.name}] selected in workspace.")
        }
    }

    fun updateEditorCode(newCode: String) {
        _editorCode.value = newCode
    }

    fun updateEditorName(newName: String) {
        _editorName.value = newName
    }

    fun updateEditorDescription(newDesc: String) {
        _editorDescription.value = newDesc
    }

    fun saveCurrentScript() {
        val current = _selectedScript.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = current.copy(
                name = _editorName.value,
                content = _editorCode.value,
                description = _editorDescription.value
            )
            repository.updateScript(updated)
            _selectedScript.value = updated
            repository.addLog("Script [${updated.name}] saved successfully.")
        }
    }

    fun createNewScript() {
        viewModelScope.launch(Dispatchers.IO) {
            val newScript = Script(
                name = "untitled_bot.py",
                description = "Custom python script",
                content = """# Custom Discord Bot
import discord
from discord.ext import commands

bot = commands.Bot(command_prefix='!')

@bot.event
async def on_ready():
    print(f'Bot {bot.user} is up and running!')

@bot.command()
async def hello(ctx):
    await ctx.send('Hello!')
"""
            )
            val newId = repository.insertScript(newScript)
            val created = newScript.copy(id = newId.toInt())
            _selectedScript.value = created
            _editorCode.value = created.content
            _editorName.value = created.name
            _editorDescription.value = created.description
            repository.addLog("Created new script [${created.name}]")
        }
    }

    fun deleteCurrentScript() {
        val current = _selectedScript.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScript(current)
            _selectedScript.value = null
            repository.addLog("Deleted script [${current.name}]")
        }
    }

    fun updateToken(newToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentConfig = repository.getBotConfig() ?: BotConfig()
            repository.updateBotConfig(currentConfig.copy(token = newToken))
        }
    }

    // Static code package scanning
    private fun scanCodeForImports(code: String, modulesList: List<ModulePackage>): List<ModulePackage> {
        val importRegex = Regex("""(?:import|from)\s+([a-zA-Z0-9_\.]+)""")
        val matches = importRegex.findAll(code)
        val importedNames = matches.map { matchResult ->
            val fullImport = matchResult.groupValues[1]
            val topLevel = fullImport.split(".")[0]
            when (topLevel) {
                "discord" -> "discord.py"
                "bs4" -> "beautifulsoup4"
                "PIL" -> "pillow"
                else -> topLevel
            }
        }.toSet()
        
        return modulesList.filter { module ->
            importedNames.contains(module.name.split(".")[0]) && !module.isInstalled
        }
    }

    // Pip Module installer redirects
    fun installModule(name: String) {
        repository.installModule(name)
    }

    fun uninstallModule(name: String) {
        repository.uninstallModule(name)
    }

    // PyPI & File Installer States
    private val _pypiSearchState = MutableStateFlow<PypiSearchStatus>(PypiSearchStatus.Idle)
    val pypiSearchState: StateFlow<PypiSearchStatus> = _pypiSearchState.asStateFlow()

    fun searchAndAddFromPyPi(packageName: String) {
        val clean = packageName.trim()
        if (clean.isEmpty()) {
            _pypiSearchState.value = PypiSearchStatus.Idle
            return
        }
        _pypiSearchState.value = PypiSearchStatus.Searching
        viewModelScope.launch {
            val result = repository.searchAndAddFromPyPi(clean)
            if (result != null) {
                _pypiSearchState.value = PypiSearchStatus.Found(result)
            } else {
                _pypiSearchState.value = PypiSearchStatus.NotFound("Package '$clean' was not found on PyPI or network is offline.")
            }
        }
    }

    fun clearSearch() {
        _pypiSearchState.value = PypiSearchStatus.Idle
    }

    fun installFromRequirementsText(content: String) {
        viewModelScope.launch {
            repository.installFromRequirementsText(content)
        }
    }

    fun installCustomFile(fileName: String, bytes: ByteArray?) {
        viewModelScope.launch {
            repository.installCustomFile(fileName, bytes)
        }
    }

    fun clearTerminalLogs() {
        repository.clearLogs()
    }

    // Run Bot Foreground Control
    fun startBot(context: Context) {
        val token = botConfig.value?.token ?: ""
        if (token.isEmpty()) {
            repository.addLog("[System ERROR] Bot token is empty. Please enter your Discord Bot Token first!")
            return
        }

        saveCurrentScript()

        val serviceIntent = Intent(context, DiscordBotService::class.java).apply {
            putExtra("TOKEN", token)
            putExtra("SCRIPT", _editorCode.value)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopBot(context: Context) {
        val serviceIntent = Intent(context, DiscordBotService::class.java).apply {
            action = "STOP"
        }
        context.startService(serviceIntent)
    }

    // Gemini AI integrations
    fun generateScriptWithAi(prompt: String) {
        _aiOperationInProgress.value = true
        repository.addLog("[AI Assistant] Generating python script using Gemini...")
        viewModelScope.launch {
            val response = geminiService.generateCode(prompt, _editorCode.value)
            _editorCode.value = response
            _aiOperationInProgress.value = false
            repository.addLog("[AI Assistant] Python code successfully updated via AI.")
        }
    }

    fun fixErrorsWithAi(errorMsg: String) {
        _aiOperationInProgress.value = true
        repository.addLog("[AI Assistant] Analyzing crash log and fixing code using Gemini...")
        viewModelScope.launch {
            val response = geminiService.fixCode(errorMsg, _editorCode.value)
            _editorCode.value = response
            _aiOperationInProgress.value = false
            repository.addLog("[AI Assistant] Bug fix completed. Script updated successfully.")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PyBotApplication
                return PyBotViewModel(application.repository) as T
            }
        }
    }
}

sealed interface PypiSearchStatus {
    object Idle : PypiSearchStatus
    object Searching : PypiSearchStatus
    data class Found(val module: ModulePackage) : PypiSearchStatus
    data class NotFound(val message: String) : PypiSearchStatus
}
