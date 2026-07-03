package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BotConfig
import com.example.data.model.ModulePackage
import com.example.data.model.Script

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PyBotViewModel) {
    val localContext = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }

    val botConfig by viewModel.botConfig.collectAsStateWithLifecycle()
    val status = botConfig?.status ?: "Offline"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "App Icon",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "PyBot Runner",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    // Status indicator dot
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (status) {
                                    "Online" -> Color(0xFF10B981)
                                    "Connecting" -> Color(0xFFF59E0B)
                                    "Error" -> Color(0xFFEF4444)
                                    else -> Color(0xFF6B7280)
                                }
                            )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F12),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F0F12),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Console") },
                    label = { Text("Console") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = "Editor") },
                    label = { Text("Workspace") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Modules") },
                    label = { Text("Modules") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
            }
        },
        containerColor = Color(0xFF0F0F12)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F0F12))
        ) {
            when (currentTab) {
                0 -> DashboardTab(
                    viewModel = viewModel,
                    onNavigateToWorkspace = { currentTab = 2 },
                    onNavigateToModules = { currentTab = 3 }
                )
                1 -> ConsoleTab(viewModel)
                2 -> WorkspaceTab(viewModel)
                3 -> ModulesTab(viewModel)
            }
        }
    }
}

@Composable
fun ConsoleTab(viewModel: PyBotViewModel) {
    val localContext = LocalContext.current
    val botConfig by viewModel.botConfig.collectAsStateWithLifecycle()
    val logs by viewModel.consoleLogs.collectAsStateWithLifecycle()

    val status = botConfig?.status ?: "Offline"
    val botName = botConfig?.botName ?: "None"
    val servers = botConfig?.activeServers ?: 0
    val messages = botConfig?.messagesProcessed ?: 0
    val uptime = botConfig?.uptimeMillis ?: 0L

    var isTokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Status overview card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Runner Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (status == "Online") botName else "Bot Offline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = when (status) {
                                "Online" -> "Connected to Gateway"
                                "Connecting" -> "Handshaking..."
                                "Error" -> "Connection Refused (Token Invalid?)"
                                else -> "Process Terminated"
                            },
                            fontSize = 12.sp,
                            color = when (status) {
                                "Online" -> Color(0xFF10B981)
                                "Connecting" -> Color(0xFFF59E0B)
                                "Error" -> Color(0xFFEF4444)
                                else -> Color.Gray
                            }
                        )
                    }

                    // Process Control Buttons
                    Row {
                        if (status != "Online" && status != "Connecting") {
                            Button(
                                onClick = { viewModel.startBot(localContext) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("START", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.stopBot(localContext) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("STOP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2E2F3E))

                // Stats grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SERVERS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("$servers", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("MESSAGES", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("$messages", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("UPTIME", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(formatUptime(uptime), fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Token config card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Discord Bot Token",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = botConfig?.token ?: "",
                    onValueChange = { viewModel.updateToken(it) },
                    placeholder = { Text("Enter bot token from developer portal...", color = Color.DarkGray, fontSize = 13.sp) },
                    singleLine = true,
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle token visibility",
                                tint = Color.Gray
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF2E2F3E)
                    ),
                    textStyle = TextStyle(fontSize = 13.sp)
                )
            }
        }

        // 3. CRT style Terminal Log Console
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0C0D14))
                .border(1.dp, Color(0xFF2E2F3E), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STDOUT / PROCESS_LOGS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearTerminalLogs() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }

                if (logs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Sandbox offline. Click START to run bot.",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    // Auto-scroll logic
                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                    ) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = when {
                                    log.contains("[Bot Error]") || log.contains("Gateway Error") || log.contains("API Error") -> Color(0xFFEF4444)
                                    log.contains("SUCCESS") || log.contains("Success") -> Color(0xFF10B981)
                                    log.contains("pip:") -> Color(0xFFF59E0B)
                                    log.contains("[AI Assistant]") -> Color(0xFF3B82F6)
                                    else -> Color(0xFFCCCCCC)
                                },
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceTab(viewModel: PyBotViewModel) {
    val localContext = LocalContext.current
    val scripts by viewModel.scripts.collectAsStateWithLifecycle()
    val selectedScript by viewModel.selectedScript.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.aiOperationInProgress.collectAsStateWithLifecycle()

    val editorCode by viewModel.editorCode.collectAsStateWithLifecycle()
    val editorName by viewModel.editorName.collectAsStateWithLifecycle()
    val editorDescription by viewModel.editorDescription.collectAsStateWithLifecycle()

    var aiPrompt by remember { mutableStateOf("") }
    var showAiAssistSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Horizontal script picker
        Text(
            text = "Active Workspace Scripts",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(scripts) { script ->
                val isSelected = selectedScript?.id == script.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectScript(script) },
                    label = { Text(script.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E293B),
                        selectedLabelColor = Color(0xFF3B82F6),
                        containerColor = Color(0xFF191A23),
                        labelColor = Color.Gray
                    )
                )
            }

            item {
                IconButton(
                    onClick = { viewModel.createNewScript() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF191A23), RoundedCornerShape(18.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create new", tint = Color.White)
                }
            }
        }

        if (selectedScript == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Select or create a script to begin editing", color = Color.Gray)
            }
        } else {
            // Script Info Editor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Script Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Row {
                            IconButton(onClick = { viewModel.saveCurrentScript() }) {
                                Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF10B981))
                            }
                            IconButton(onClick = { viewModel.deleteCurrentScript() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editorName,
                        onValueChange = { viewModel.updateEditorName(it) },
                        label = { Text("Script Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF2E2F3E)
                        )
                    )

                    OutlinedTextField(
                        value = editorDescription,
                        onValueChange = { viewModel.updateEditorDescription(it) },
                        label = { Text("Script Description", fontSize = 12.sp) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF2E2F3E)
                        )
                    )
                }
            }

            // Code Canvas editor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0D14)),
                border = BorderStroke(1.dp, Color(0xFF2E2F3E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = editorCode,
                        onValueChange = { viewModel.updateEditorCode(it) },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE2E8F0),
                            unfocusedTextColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        placeholder = {
                            Text(
                                "# Write python code here...",
                                fontFamily = FontFamily.Monospace,
                                color = Color.DarkGray,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // Quick templates button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Code Quick-Templates:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                Row {
                    Button(
                        onClick = {
                            viewModel.updateEditorCode(getPingTemplate())
                            Toast.makeText(localContext, "Loaded Ping Template", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("Ping Bot", fontSize = 10.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            viewModel.updateEditorCode(getModeratorTemplate())
                            Toast.makeText(localContext, "Loaded Moderation Template", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Moderator Bot", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            // Gemini AI Assistant Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2433)),
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gemini AI Assistant", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }

                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF60A5FA))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Prompt Gemini to edit your script, add new features, or automatically fix run errors.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        placeholder = { Text("e.g. 'Add a kick and ban moderator command prefix !'", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAiLoading,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (aiPrompt.isNotBlank()) {
                                        viewModel.generateScriptWithAi(aiPrompt)
                                        aiPrompt = ""
                                    }
                                },
                                enabled = !isAiLoading && aiPrompt.isNotBlank()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF60A5FA))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF60A5FA),
                            unfocusedBorderColor = Color(0xFF2E2F3E)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { viewModel.fixErrorsWithAi("IndentationError: unexpected indent or process crash") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F3F46)),
                            shape = RoundedCornerShape(6.dp),
                            enabled = !isAiLoading,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Fix", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Auto-Fix Bug", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesTab(viewModel: PyBotViewModel) {
    val context = LocalContext.current
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val missingDetected by viewModel.missingDetectedModules.collectAsStateWithLifecycle()
    val pypiSearchState by viewModel.pypiSearchState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }

    // Search query for local repository
    var searchQuery by remember { mutableStateOf("") }
    val filteredModules = remember(searchQuery, modules) {
        if (searchQuery.isBlank()) {
            modules
        } else {
            modules.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Input for PyPI live registry search
    var pypiSearchInput by remember { mutableStateOf("") }

    // File picker launcher supporting .whl, .py, and requirements.txt
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                var fileName = "unknown_file"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val extension = fileName.substringAfterLast(".").lowercase()
                    
                    if (extension == "txt" || fileName == "requirements.txt") {
                        val textContent = String(bytes, Charsets.UTF_8)
                        viewModel.installFromRequirementsText(textContent)
                        Toast.makeText(context, "Installing packages from requirements.txt...", Toast.LENGTH_LONG).show()
                        activeTab = 0 // Navigate to cache tab to view installation log and progress
                    } else if (extension == "whl" || extension == "py") {
                        viewModel.installCustomFile(fileName, bytes)
                        Toast.makeText(context, "Registered local library: $fileName", Toast.LENGTH_LONG).show()
                        activeTab = 0 // Navigate back to cache
                    } else {
                        Toast.makeText(context, "Unsupported file format. Please upload .whl, .py, or requirements.txt", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Row selector for sources
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Repository Cache", "PyPI Web Search", "File Installer").forEachIndexed { index, label ->
                FilterChip(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E293B),
                        selectedLabelColor = Color(0xFF3B82F6),
                        containerColor = Color(0xFF191A23),
                        labelColor = Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (activeTab) {
            0 -> {
                // TAB 0: REPOSITORY CACHE / MAIN LIST
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search package cache...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF191A23)
                        )
                    )

                    // Static script analyzer recommendations
                    AnimatedVisibility(
                        visible = missingDetected.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7C2D12)),
                            border = BorderStroke(1.dp, Color(0xFFF97316)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFF97316))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Imports Scanner Alert!",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Your current script imports modules that are not installed on this device: " +
                                            "${missingDetected.joinToString { it.name }}",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    missingDetected.forEach { missingMod ->
                                        Button(
                                            onClick = { viewModel.installModule(missingMod.name) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.padding(start = 6.dp)
                                        ) {
                                            Text("Install ${missingMod.name}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Cache Header
                    Text(
                        text = "Module Cache Repository",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (filteredModules.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E2F3E))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ManageSearch, contentDescription = "No local matches", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No local matches for '$searchQuery'", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Would you like to search PyPI live registry online for this package instead?", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        pypiSearchInput = searchQuery
                                        activeTab = 1
                                        viewModel.searchAndAddFromPyPi(searchQuery)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = "PyPI", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Search on PyPI", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredModules) { module ->
                                ModuleCard(
                                    module = module,
                                    onInstallClick = { viewModel.installModule(module.name) },
                                    onUninstallClick = { viewModel.uninstallModule(module.name) }
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                // TAB 1: PyPI REGISTRY LIVE SEARCH
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = pypiSearchInput,
                        onValueChange = { pypiSearchInput = it },
                        placeholder = { Text("Enter PyPI package name (e.g. sympy, httpx, jinja2)...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = "PyPI Search", tint = Color(0xFF3B82F6)) },
                        trailingIcon = {
                            if (pypiSearchInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    pypiSearchInput = ""
                                    viewModel.clearSearch()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF191A23)
                        )
                    )

                    Button(
                        onClick = { viewModel.searchAndAddFromPyPi(pypiSearchInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pypiSearchInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search icon")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search PyPI Live Registry", fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = Color(0xFF1E202E), modifier = Modifier.padding(vertical = 4.dp))

                    when (val state = pypiSearchState) {
                        is PypiSearchStatus.Idle -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Web Index", tint = Color.DarkGray, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Online Python Package Index", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Look up and dynamically retrieve dependency modules straight from official PyPI servers.", color = Color.Gray, fontSize = 11.sp)
                                
                                Spacer(modifier = Modifier.height(18.dp))
                                Text("POPULAR SEARCHES:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("httpx", "sympy", "gspread", "pytz", "feedparser").forEach { name ->
                                        SuggestionChip(
                                            onClick = {
                                                pypiSearchInput = name
                                                viewModel.searchAndAddFromPyPi(name)
                                            },
                                            label = { Text(name, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                        is PypiSearchStatus.Searching -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Retrieving metadata from PyPI servers...", color = Color.LightGray, fontSize = 12.sp)
                                Text("https://pypi.org/pypi/$pypiSearchInput/json", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        is PypiSearchStatus.Found -> {
                            val module = state.module
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Language, contentDescription = "Registry", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Found on PyPI", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 11.sp)
                                            }
                                            Text(module.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(module.size, fontSize = 10.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Release Version: v${module.version}",
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = module.description,
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        maxLines = 4
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFF1E202E))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.installModule(module.name)
                                                Toast.makeText(context, "Downloading package ${module.name}...", Toast.LENGTH_SHORT).show()
                                                activeTab = 0 // jump to local list to see progress
                                                searchQuery = module.name
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = "Add package")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add & Install Package", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        is PypiSearchStatus.NotFound -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1E1E)),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = "Not found", tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Package Not Found", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                        Text(state.message, color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // TAB 2: FILE INSTALLER
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23)),
                        border = BorderStroke(1.dp, Color(0xFF2E2F3E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = "Files", tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install dependencies from files", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Bulk-install multiple packages listed in a requirements.txt file, or directly import and register standard Python packages (.whl) or code scripts (.py).",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Bulk requirements.txt
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                        border = BorderStroke(1.dp, Color(0xFF1E202E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Bulk requirements.txt List", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("Load requirements.txt to install all libraries in a single sweep.", color = Color.Gray, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.PlaylistAdd, contentDescription = "List", tint = Color(0xFF3B82F6))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Upload", tint = Color(0xFF3B82F6))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select requirements.txt File", color = Color.White)
                            }
                        }
                    }

                    // Local custom files (.whl / .py)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
                        border = BorderStroke(1.dp, Color(0xFF1E202E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Local Wheel (.whl) or Custom Module (.py)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("Register a custom code package archive or library script file directly.", color = Color.Gray, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.SettingsInputComponent, contentDescription = "Wheel", tint = Color(0xFF10B981))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Folder", tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select .whl / .py File", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModuleCard(
    module: ModulePackage,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2F3E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = module.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "v${module.version}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Category Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(module.category.uppercase(), fontSize = 9.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = module.description,
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp), color = Color(0xFF2E2F3E))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Size metric
                Text(
                    text = "Size: ${module.size}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                // Download/Install Button / Progress State
                when {
                    module.isInstalled -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Installed", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Installed", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onUninstallClick, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    module.downloadProgress > 0.0f && module.downloadProgress < 1.0f -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(0.7f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Installing... ${(module.downloadProgress * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    color = Color(0xFFF59E0B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { module.downloadProgress },
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFFF59E0B),
                                    trackColor = Color(0xFF2E2F3E)
                                )
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onInstallClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Download", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Format milliseconds uptime to readable HH:mm:ss
private fun formatUptime(millis: Long): String {
    if (millis == 0L) return "00:00:00"
    val secs = millis / 1000
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

// Built-in templates
private fun getPingTemplate(): String {
    return """# Simple Ping Bot
import discord
from discord.ext import commands

bot = commands.Bot(command_prefix='!')

@bot.event
async def on_ready():
    print(f'Ping Bot is online as {bot.user}!')

@bot.command()
async def ping(ctx):
    ""\"Responds with pong and latency""\"
    latency = round(bot.latency * 1000)
    await ctx.send(f'Pong! 🏓 ({latency}ms)')

@bot.command()
async def stats(ctx):
    ""\"Show basic bot stats""\"
    await ctx.send('Running smoothly on PyBot Android engine.')
"""
}

private fun getModeratorTemplate(): String {
    return """# Moderation and Security Bot
import discord
from discord.ext import commands

bot = commands.Bot(command_prefix='!')

@bot.event
async def on_ready():
    print(f'Moderator Bot active as {bot.user}')

@bot.command()
@commands.has_permissions(kick_members=True)
async def kick(ctx, member: discord.Member, *, reason=None):
    ""\"Kicks a member from the server""\"
    await member.kick(reason=reason)
    await ctx.send(f'Success: {member.name} has been kicked.')

@bot.command()
@commands.has_permissions(ban_members=True)
async def ban(ctx, member: discord.Member, *, reason=None):
    ""\"Bans a member from the server""\"
    await member.ban(reason=reason)
    await ctx.send(f'Success: {member.name} has been banned.')

@bot.command()
async def rules(ctx):
    ""\"Prints server rules""\"
    await ctx.send('1. Be respectful\n2. No spam\n3. Obey staff')
"""
}
