package com.voicereminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgColor = Color(0xFFF9F9FA)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1D1D1F)
private val TextSecondary = Color(0xFF86868B)
private val AccentColor = Color(0xFF5E5CE6)
private val DividerColor = Color(0xFFE5E5EA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    // Preserve the existing mocked state logic as the original implementation,
    // but expose them via appropriate UI elements (toggle vs chevron).
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var serverUrl by rememberSaveable { mutableStateOf(com.voicereminder.network.AuthManager.baseUrl) }
    var showServerDialog by rememberSaveable { mutableStateOf(false) }
    var tempUrlDraft by rememberSaveable { mutableStateOf(serverUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // SERVER CONNECTION
            item {
                SettingsSection(title = "SERVER CONNECTION") {
                    SettingRow(
                        icon = Icons.Default.CloudQueue,
                        title = "Backend Server URL",
                        description = serverUrl,
                        type = SettingType.Navigation,
                        onClick = {
                            tempUrlDraft = serverUrl
                            showServerDialog = true
                        }
                    )
                }
            }

            // VOICE
            item {
                SettingsSection(title = "VOICE") {
                    SettingRow(
                        icon = Icons.Default.Mic,
                        title = "Voice & Wake Word",
                        description = "Configure voice input and wake behavior",
                        type = SettingType.Navigation
                    )
                }
            }

            // AI
            item {
                SettingsSection(title = "AI") {
                    SettingRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI Behavior",
                        description = "Configure assistant behavior and responses",
                        type = SettingType.Navigation
                    )
                }
            }

            // NOTIFICATIONS
            item {
                SettingsSection(title = "NOTIFICATIONS") {
                    SettingRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        description = "Manage reminder and task alerts",
                        type = SettingType.Toggle(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    )
                }
            }

            // MEMORY
            item {
                SettingsSection(title = "MEMORY") {
                    SettingRow(
                        icon = Icons.Default.Psychology,
                        title = "Memory",
                        description = "Manage what VoiceReminder remembers",
                        type = SettingType.Navigation
                    )
                }
            }

            // PREFERENCES
            item {
                SettingsSection(title = "PREFERENCES") {
                    SettingRow(
                        icon = Icons.Default.Language,
                        title = "Language",
                        description = null,
                        type = SettingType.ValueAndNavigation("English")
                    )
                    SettingsDivider()
                    SettingRow(
                        icon = Icons.Default.Palette,
                        title = "Appearance",
                        description = null,
                        type = SettingType.ValueAndNavigation("System default")
                    )
                }
            }

            // DATA & PRIVACY
            item {
                SettingsSection(title = "DATA & PRIVACY") {
                    SettingRow(
                        icon = Icons.Default.Shield,
                        title = "Privacy & Permissions",
                        description = "Manage microphone and app permissions",
                        type = SettingType.Navigation
                    )
                    SettingsDivider()
                    SettingRow(
                        icon = Icons.Default.Storage,
                        title = "Data & Storage",
                        description = "Manage local data and storage",
                        type = SettingType.Navigation
                    )
                }
            }

            // ABOUT
            item {
                SettingsSection(title = "ABOUT") {
                    SettingRow(
                        icon = Icons.Default.Info,
                        title = "About VoiceReminder",
                        description = null,
                        type = SettingType.ValueAndNavigation("Version 1.0.0")
                    )
                    SettingsDivider()
                    SettingRow(
                        icon = Icons.Default.Article,
                        title = "Privacy Policy",
                        description = null,
                        type = SettingType.Navigation
                    )
                    SettingsDivider()
                    SettingRow(
                        icon = Icons.Default.Gavel,
                        title = "Terms of Service",
                        description = null,
                        type = SettingType.Navigation
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Backend Server URL", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(
                        "Set the server address. For universal access on any network or college WiFi, use the public URL:\n• https://swerve-buddhist-swaddling.ngrok-free.dev/\n• Or your Mac's IP (e.g. http://192.168.21.182:9000/)",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempUrlDraft,
                        onValueChange = { tempUrlDraft = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUrl = if (tempUrlDraft.endsWith("/")) tempUrlDraft else "$tempUrlDraft/"
                        com.voicereminder.network.AuthManager.baseUrl = finalUrl
                        serverUrl = finalUrl
                        showServerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = SurfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = DividerColor,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp)
    )
}

private sealed class SettingType {
    object Navigation : SettingType()
    data class ValueAndNavigation(val value: String) : SettingType()
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : SettingType()
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    description: String?,
    type: SettingType,
    onClick: (() -> Unit)? = null
) {
    val isClickable = type is SettingType.Navigation || type is SettingType.ValueAndNavigation || onClick != null
    val clickModifier = if (isClickable) {
        Modifier.clickable {
            if (type is SettingType.Toggle) {
                type.onCheckedChange(!type.checked)
            } else {
                onClick?.invoke()
            }
        }
    } else {
        if (type is SettingType.Toggle) {
            Modifier.clickable { type.onCheckedChange(!type.checked) }
        } else {
            Modifier
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        when (type) {
            is SettingType.Navigation -> {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            is SettingType.ValueAndNavigation -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = type.value,
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            is SettingType.Toggle -> {
                Switch(
                    checked = type.checked,
                    onCheckedChange = type.onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceColor,
                        checkedTrackColor = AccentColor,
                        uncheckedThumbColor = SurfaceColor,
                        uncheckedTrackColor = DividerColor,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}
