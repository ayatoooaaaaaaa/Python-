package com.example.ui

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BotConfig
import com.example.data.model.ModulePackage
import com.example.data.model.Script

@Composable
fun DashboardTab(
    viewModel: PyBotViewModel,
    onNavigateToWorkspace: () -> Unit,
    onNavigateToModules: () -> Unit
) {
    val context = LocalContext.current
    val botConfig by viewModel.botConfig.collectAsStateWithLifecycle()
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val selectedScript by viewModel.selectedScript.collectAsStateWithLifecycle()

    val status = botConfig?.status ?: "Offline"
    val botName = botConfig?.botName ?: "None"
    val servers = botConfig?.activeServers ?: 0
    val messages = botConfig?.messagesProcessed ?: 0
    val uptime = botConfig?.uptimeMillis ?: 0L

    val installedModules = remember(modules) { modules.filter { it.isInstalled } }

    // Pulsing alpha for live status dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "System Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Text(
                    text = "Real-time engine metrics & package manager",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Quick Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        when (status) {
                            "Online" -> Color(0xFF10B981).copy(alpha = 0.15f)
                            "Connecting" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            "Error" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            else -> Color(0xFF6B7280).copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        1.dp,
                        when (status) {
                            "Online" -> Color(0xFF10B981)
                            "Connecting" -> Color(0xFFF59E0B)
                            "Error" -> Color(0xFFEF4444)
                            else -> Color(0xFF6B7280)
                        },
                        RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(if (status == "Online" || status == "Connecting") pulseAlpha else 1.0f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (status) {
                                "Online" -> Color(0xFF10B981)
                                "Connecting" -> Color(0xFFF59E0B)
                                "Error" -> Color(0xFFEF4444)
                                else -> Color(0xFF9CA3AF)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (status) {
                        "Online" -> Color(0xFF10B981)
                        "Connecting" -> Color(0xFFF59E0B)
                        "Error" -> Color(0xFFEF4444)
                        else -> Color(0xFF9CA3AF)
                    }
                )
            }
        }

        // Main Discord Bot Status Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2E2F3E))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (status == "Online") botName else "Bot Instance Offline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = when (status) {
                                "Online" -> "Connected to Discord Gateway API"
                                "Connecting" -> "Handshaking with Discord API..."
                                "Error" -> "Gateway Error (Check Token)"
                                else -> "Process is currently suspended"
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Foreground Bot Controller
                    if (status != "Online" && status != "Connecting") {
                        Button(
                            onClick = { viewModel.startBot(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN BOT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopBot(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STOP BOT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF2E2F3E))

                // Stats Dashboard Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DashboardStatItem(
                        icon = Icons.Default.Dns,
                        title = "GUILDS",
                        value = if (status == "Online") "$servers" else "--",
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatItem(
                        icon = Icons.Default.ChatBubble,
                        title = "MESSAGES",
                        value = if (status == "Online") "$messages" else "--",
                        color = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatItem(
                        icon = Icons.Default.Schedule,
                        title = "UPTIME",
                        value = if (status == "Online") formatUptime(uptime) else "00:00:00",
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Active script in workspace status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12131C)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E202E)),
            onClick = onNavigateToWorkspace
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Active Script",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Workspace Script",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedScript?.name ?: "No Script Selected",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit script",
                    tint = Color.Gray
                )
            }
        }

        // Installed Modules Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LibraryBooks,
                        contentDescription = "Modules",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Installed PIP Modules (${installedModules.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = "Manage List",
                    color = Color(0xFF3B82F6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToModules() }
                        .padding(4.dp)
                )
            }

            if (installedModules.isEmpty()) {
                // Empty State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E2F3E).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty Modules",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No packages installed yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Your Python environment needs libraries to operate.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = onNavigateToModules,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Browse", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Browse Package Repository", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Modules list layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    installedModules.forEach { module ->
                        InstalledModuleRow(
                            module = module,
                            onUninstall = {
                                viewModel.uninstallModule(module.name)
                                Toast.makeText(context, "${module.name} uninstalled.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstalledModuleRow(
    module: ModulePackage,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191A23)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2F3E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Module icon representation based on package name
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F0F12)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (module.name) {
                            "discord.py" -> Icons.Default.SmartToy
                            "beautifulsoup4" -> Icons.Default.Search
                            "pillow" -> Icons.Default.Image
                            "requests" -> Icons.Default.Http
                            else -> Icons.Default.SettingsInputComponent
                        },
                        contentDescription = "Module type",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = module.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "v${module.version}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = module.description,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Size badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = module.size,
                        fontSize = 9.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Uninstall Module",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
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
