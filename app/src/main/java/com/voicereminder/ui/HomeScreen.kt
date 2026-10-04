package com.voicereminder.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicereminder.network.MobileCommandResponse
import com.voicereminder.ui.theme.*
import com.voicereminder.viewmodel.ChatMessage
import com.voicereminder.viewmodel.MessageSender
import com.voicereminder.viewmodel.RecordingState
import java.util.Locale

@Composable
private fun ModernStatPill(label: String, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Navy700.copy(alpha = 0.78f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModernTodayCard(
    serverReachable: Boolean?,
    pendingCount: Int,
    onNavigateToList: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
) {
    val statusLabel = when (serverReachable) {
        true -> "Backend online"
        false -> "Backend offline"
        null -> "Connecting to backend"
    }
    val statusColor = when (serverReachable) {
        true -> GreenSuccess
        false -> RedError
        null -> OrangeWarning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Navy800.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, Navy600.copy(alpha = 0.55f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Today",
                        color = White,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusLabel,
                        color = Grey500,
                        fontSize = 12.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$pendingCount items",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Divider(color = Navy600.copy(alpha = 0.45f))
            Spacer(Modifier.height(14.dp))

            Text(
                text = "• 2 reminders due today\n• 1 document to review\n• 3 workflows waiting",
                color = Grey300,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModernActionChip(text = "Reminders", icon = Icons.AutoMirrored.Filled.List, onClick = onNavigateToList)
                ModernActionChip(text = "Workspace", icon = Icons.Default.Workspaces, onClick = onNavigateToWorkspace)
            }
        }
    }
}

@Composable
private fun ModernActionChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Navy700.copy(alpha = 0.75f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, Navy600.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Violet400, modifier = Modifier.size(16.dp))
            Text(text, color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ModernCapabilityRail() {
    val capabilities = listOf(
        CapabilityItem("Tasks", Icons.Default.TaskAlt),
        CapabilityItem("Notes", Icons.Default.StickyNote2),
        CapabilityItem("Docs", Icons.Default.Description),
        CapabilityItem("Research", Icons.Default.Search),
        CapabilityItem("Schedule", Icons.Default.Event),
        CapabilityItem("Translate", Icons.Default.Translate),
        CapabilityItem("Memory", Icons.Default.Psychology),
        CapabilityItem("Workflows", Icons.Default.Route),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Capabilities",
            color = Grey500,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            capabilities.take(4).forEach { item -> ModernCapabilityTile(item, Modifier.weight(1f)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            capabilities.drop(4).forEach { item -> ModernCapabilityTile(item, Modifier.weight(1f)) }
        }
    }
}

private data class CapabilityItem(val label: String, val icon: ImageVector)

@Composable
private fun ModernCapabilityTile(item: CapabilityItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Navy800.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, Navy600.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Violet500.copy(alpha = 0.12f))
                    .border(1.dp, Violet500.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.icon, contentDescription = null, tint = Violet400, modifier = Modifier.size(19.dp))
            }
            Text(
                text = item.label,
                color = White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ModernAppGrid(
    onNavigateToList: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
) {
    val apps = listOf(
        AppItem("Tasks", Icons.Default.TaskAlt),
        AppItem("Notes", Icons.Default.NoteAlt),
        AppItem("Docs", Icons.Default.Description),
        AppItem("Research", Icons.Default.Search),
        AppItem("Schedule", Icons.Default.Event),
        AppItem("Translate", Icons.Default.Language),
        AppItem("Memory", Icons.Default.Psychology),
        AppItem("Workflows", Icons.Default.AccountTree),
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            apps.take(4).forEach { app -> ModernAppIcon(app, Modifier.weight(1f)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            apps.drop(4).forEachIndexed { index, app ->
                ModernAppIcon(
                    app,
                    Modifier.weight(1f),
                    onClick = when (index) {
                        0 -> onNavigateToList
                        1 -> onNavigateToWorkspace
                        else -> null
                    }
                )
            }
        }
    }
}

private data class AppItem(val label: String, val icon: ImageVector)

@Composable
private fun ModernAppIcon(
    app: AppItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(62.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(18.dp),
            color = Navy800.copy(alpha = 0.74f),
            border = BorderStroke(1.dp, Navy600.copy(alpha = 0.55f)),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(app.icon, contentDescription = app.label, tint = Violet400, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(app.label, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModernRecentStrip(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Recent",
            color = Grey500,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold,
        )

        messages.forEach { message ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Navy800.copy(alpha = 0.68f),
                border = BorderStroke(1.dp, Navy600.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (message.isError) RedError else Violet500)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = message.text,
                        color = White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernAssistantSurface(
    recordingState: RecordingState,
    liveTranscript: String,
    voiceRmsDb: Float,
    recordingStartedAtMs: Long?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onSubmitText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetHeight = when (recordingState) {
        RecordingState.Idle -> 78.dp
        RecordingState.Activating -> 256.dp
        RecordingState.Recording -> 256.dp
        RecordingState.Processing -> 286.dp
        is RecordingState.Success -> 238.dp
        is RecordingState.Error -> 200.dp
    }

    val surfaceHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "assistantHeight",
    )

    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val borderColor = when (recordingState) {
        RecordingState.Idle -> Navy600.copy(alpha = 0.7f)
        RecordingState.Activating -> OrangeWarning.copy(alpha = 0.45f)
        RecordingState.Recording -> OrangeWarning.copy(alpha = 0.45f)
        RecordingState.Processing -> Violet400.copy(alpha = 0.45f)
        is RecordingState.Success -> GreenSuccess.copy(alpha = 0.4f)
        is RecordingState.Error -> RedError.copy(alpha = 0.45f)
    }

    var manualText by remember { mutableStateOf("") }
    val isExpandedState = recordingState != RecordingState.Idle

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, shape, clip = false)
            .border(1.dp, borderColor, shape),
        shape = shape,
        color = Navy800.copy(alpha = 0.96f),
    ) {
        Column(
            modifier = Modifier
                .height(surfaceHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModernAssistantHandle(recordingState = recordingState)

                AnimatedContent(targetState = recordingState, label = "assistant_state") { state ->
                    when (state) {
                        RecordingState.Idle -> ModernIdleAssistantState(onStartRecording = onStartRecording)
                        RecordingState.Activating -> ModernActivationAssistantState(
                            liveTranscript = liveTranscript,
                            onCancelRecording = onCancelRecording,
                        )
                        RecordingState.Recording -> ModernListeningAssistantState(
                            liveTranscript = liveTranscript,
                            voiceRmsDb = voiceRmsDb,
                            recordingStartedAtMs = recordingStartedAtMs,
                            onStopRecording = onStopRecording,
                            onCancelRecording = onCancelRecording,
                        )
                        RecordingState.Processing -> ModernProcessingAssistantState(liveTranscript = liveTranscript)
                        is RecordingState.Success -> ModernSuccessAssistantState(response = state.response)
                        is RecordingState.Error -> ModernErrorAssistantState(
                            message = state.message,
                            onStartRecording = onStartRecording,
                        )
                    }
                }
            }

            if (!isExpandedState) {
                ModernCommandComposer(
                    value = manualText,
                    onValueChange = { manualText = it },
                    onSend = {
                        if (manualText.isNotBlank()) {
                            onSubmitText(manualText)
                            manualText = ""
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ModernAssistantHandle(recordingState: RecordingState) {
    val transition = rememberInfiniteTransition(label = "assistant_handle")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = if (recordingState is RecordingState.Activating || recordingState is RecordingState.Recording) 1f else 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (recordingState is RecordingState.Activating || recordingState is RecordingState.Recording) 800 else 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "handlePulse",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Violet500.copy(alpha = pulse)),
        )
    }
}

@Composable
private fun ModernActivationAssistantState(
    liveTranscript: String,
    onCancelRecording: () -> Unit,
) {
    ModernWakeListeningCore(
        title = "Listening",
        subtitle = if (liveTranscript.isBlank()) "Wake word detected." else liveTranscript,
        accent = OrangeWarning,
        onCancelRecording = onCancelRecording,
        ribbonLevel = 0.42f,
        showTranscript = false,
    )
}

@Composable
private fun ModernIdleAssistantState(
    onStartRecording: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onStartRecording,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Violet500)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Voice available",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Say a wake phrase or tap to start",
                    color = Grey500,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = "Ready",
            color = Violet400,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ModernListeningAssistantState(
    liveTranscript: String,
    voiceRmsDb: Float,
    recordingStartedAtMs: Long?,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(OrangeWarning.copy(alpha = 0.12f))
                        .border(1.dp, OrangeWarning.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = OrangeWarning,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Recording",
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = rememberRecordingTicker(recordingStartedAtMs),
                        color = Grey500,
                        fontSize = 12.sp,
                    )
                }
            }

            ModernMiniActionButton(text = "Cancel", onClick = onCancelRecording)
        }

        ModernVoiceRibbon(rmsDb = voiceRmsDb, accent = OrangeWarning)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (liveTranscript.isBlank()) "Speak naturally..." else liveTranscript,
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(OrangeWarning)
            )
            Text(
                text = "Live voice input",
                color = Grey500,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            ModernMiniActionButton(text = "Stop", onClick = onStopRecording)
        }
    }
}

@Composable
private fun ModernWakeListeningCore(
    title: String,
    subtitle: String,
    accent: Color,
    onCancelRecording: () -> Unit,
    ribbonLevel: Float,
    showTranscript: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = Grey500, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            ModernMiniActionButton(text = "Cancel", onClick = onCancelRecording)
        }

        ModernVoiceRibbon(rmsDb = ribbonLevel * 4f - 2f, accent = accent)

        if (showTranscript) {
            Text(
                text = subtitle,
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModernVoiceRibbon(
    rmsDb: Float,
    accent: Color,
) {
    val bars = listOf(0.20f, 0.30f, 0.42f, 0.56f, 0.46f, 0.32f, 0.22f)
    val level = ((rmsDb + 2f) / 8f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Navy700.copy(alpha = 0.55f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bars.forEachIndexed { index, base ->
                val phase = ((index - 3).toFloat() * 0.08f) + level
                val animated = animateFloatAsState(
                    targetValue = (base + phase).coerceIn(0.12f, 0.92f),
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "voiceRibbonBar$index",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((animated.value * 11f).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.24f + (animated.value * 0.24f))),
                )
            }
        }
    }
}

@Composable
private fun rememberRecordingTicker(startedAtMs: Long?): String {
    var elapsed by remember(startedAtMs) { mutableStateOf(0L) }
    LaunchedEffect(startedAtMs) {
        if (startedAtMs == null) {
            elapsed = 0L
            return@LaunchedEffect
        }
        while (true) {
            elapsed = System.currentTimeMillis() - startedAtMs
            kotlinx.coroutines.delay(1000)
        }
    }
    return formatElapsedRecordingTime(elapsed)
}

private fun formatElapsedRecordingTime(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
private fun ModernProcessingAssistantState(liveTranscript: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernStateHeadline("Executing")
        ModernTranscriptCard(
            title = if (liveTranscript.isBlank()) "Working through the request" else liveTranscript,
            subtitle = "Understanding intent, planning the steps, and completing the task.",
            accent = Violet400,
            showLoading = true,
        )
        ModernStepRow("Understanding request", active = true)
        ModernStepRow("Checking reminders, notes, and schedule")
        ModernStepRow("Preparing the response")
    }
}

@Composable
private fun ModernSuccessAssistantState(response: MobileCommandResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernStateHeadline("Done")
        ModernTranscriptCard(
            title = response.replyText.ifBlank { "Task completed" },
            subtitle = response.intent?.let { "Intent: $it" } ?: "Completed successfully inside the assistant surface.",
            accent = GreenSuccess,
            showLoading = false,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            response.task?.takeIf { it.isNotBlank() }?.let { ModernStatusChip(it) }
            response.scheduledAt?.takeIf { it.isNotBlank() }?.let { ModernStatusChip("Scheduled") }
            response.confidence.takeIf { it > 0f }?.let { ModernStatusChip("${(it * 100).toInt()}% confidence") }
        }
    }
}

@Composable
private fun ModernErrorAssistantState(
    message: String,
    onStartRecording: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernStateHeadline("Needs attention")
        ModernTranscriptCard(
            title = message,
            subtitle = "Try again or rephrase the request.",
            accent = RedError,
            showLoading = false,
        )
        ModernMiniActionButton(text = "Try again", onClick = onStartRecording)
    }
}

@Composable
private fun ModernStateHeadline(text: String) {
    Text(
        text = text,
        color = White,
        fontFamily = FontFamily.Serif,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ModernTranscriptCard(
    title: String,
    subtitle: String,
    accent: Color,
    showLoading: Boolean,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Navy700.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Grey500,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun ModernStepRow(text: String, active: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) Violet400 else Navy600)
        )
        Text(
            text = text,
            color = if (active) White else Grey500,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ModernMiniActionButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Navy700.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, Navy600.copy(alpha = 0.55f)),
    ) {
        Text(
            text = text,
            color = White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun ModernStatusChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Navy700.copy(alpha = 0.8f))
            .border(1.dp, Navy600.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(text, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModernBottomNavigationPill() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(124.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Navy600)
        )
    }
}

@Composable
private fun ModernCommandComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Type a command, note, or question", color = Grey500) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violet400,
            unfocusedBorderColor = Navy600,
            focusedTextColor = White,
            unfocusedTextColor = White,
            cursorColor = Violet400,
            focusedContainerColor = Navy800,
            unfocusedContainerColor = Navy800,
        ),
        trailingIcon = {
            IconButton(onClick = onSend) {
                Icon(Icons.Default.Search, contentDescription = "Send", tint = Violet400)
            }
        },
    )
}

// ============================================================
// Status text above orb
// ============================================================
@Composable
private fun StatusText(isRecording: Boolean, isProcessing: Boolean, liveTranscript: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation   = tween(800, easing = FastOutSlowInEasing),
            repeatMode  = RepeatMode.Reverse
        ),
        label = "status_alpha"
    )

    Box(
        modifier           = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        contentAlignment   = Alignment.Center
    ) {
        when {
            isRecording -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RedError.copy(alpha = alpha))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = if (liveTranscript.isNotBlank()) "\"$liveTranscript\"" else "Listening…",
                    color    = if (liveTranscript.isNotBlank()) White else Grey500,
                    fontSize = 15.sp,
                    fontStyle = if (liveTranscript.isBlank()) FontStyle.Italic else FontStyle.Normal,
                    fontWeight = FontWeight.Medium
                )
            }
            isProcessing -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color       = Violet400,
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Processing…", color = Violet300, fontSize = 14.sp)
            }
            else -> {}
        }
    }
}

// ============================================================
// Chat history list
// ============================================================
@Composable
private fun ChatHistoryList(
    messages       : List<ChatMessage>,
    liveTranscript : String,
    isRecording    : Boolean,
    isProcessing   : Boolean,
) {
    val listState = rememberLazyListState()

    // Scroll to bottom whenever messages or transcript changes
    val totalItems = messages.size + (if (liveTranscript.isNotBlank()) 1 else 0)
    LaunchedEffect(totalItems) {
        if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }

    LazyColumn(
        state           = listState,
        modifier        = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding  = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(messages, key = { it.id }) { msg ->
            AnimatedVisibility(
                visible  = true,
                enter    = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            ) {
                ChatBubble(message = msg)
            }
        }

        // Live transcript bubble — visible during recording AND processing
        if (liveTranscript.isNotBlank() && (isRecording || isProcessing)) {
            item(key = "live") {
                ChatBubble(
                    message = ChatMessage(sender = MessageSender.USER, text = liveTranscript),
                    isLive  = isRecording,   // "..." only when still recording
                )
            }
        }

        // "Thinking" assistant bubble while waiting for response
        if (isProcessing) {
            item(key = "thinking") {
                ThinkingBubble()
            }
        }
    }
}

// ============================================================
// Chat bubble
// ============================================================
@Composable
private fun ChatBubble(message: ChatMessage, isLive: Boolean = false) {
    val isUser    = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    val bgColor = when {
        message.isError -> RedError.copy(alpha = 0.15f)
        isUser          -> Violet500
        else            -> Navy800
    }
    val textColor = when {
        message.isError -> RedError
        isUser          -> White
        else            -> Grey300
    }
    val shape = if (isUser)
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    else
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape    = shape,
            color    = bgColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text      = message.text + if (isLive) "…" else "",
                color     = textColor,
                fontSize  = 15.sp,
                modifier  = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

// ============================================================
// Animated "thinking" bubble for assistant
// ============================================================
@Composable
private fun ThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val dotOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "d1"
    )
    val dotOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 130), RepeatMode.Reverse),
        label = "d2"
    )
    val dotOffset3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 260), RepeatMode.Reverse),
        label = "d3"
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
            color = Navy800,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dotOffset1, dotOffset2, dotOffset3).forEach { offset ->
                    Box(
                        Modifier
                            .size(8.dp)
                            .offset(y = offset.dp)
                            .clip(CircleShape)
                            .background(Violet300)
                    )
                }
            }
        }
    }
}

// ============================================================
// App Header
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppHeader(
    serverReachable      : Boolean?,
    pendingCount         : Int,
    onNavigateToList     : () -> Unit,
    onNavigateToWorkspace: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text("Voicebox", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val (dot, label) = when (serverReachable) {
                        true  -> GreenSuccess to "Online"
                        false -> RedError to "Offline"
                        null  -> OrangeWarning to "Connecting…"
                    }
                    Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
                    Text(label, color = Grey500, fontSize = 11.sp)
                }
            }
        },
        actions = {
            BadgedBox(
                badge = {
                    if (pendingCount > 0) {
                        Badge(containerColor = Violet500) {
                            Text("$pendingCount", color = White, fontSize = 10.sp)
                        }
                    }
                },
            ) {
                IconButton(onClick = onNavigateToList) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Reminders", tint = Grey300)
                }
            }
            IconButton(onClick = onNavigateToWorkspace) {
                Icon(Icons.Default.Dashboard, contentDescription = "Workspace", tint = Grey300)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
    )
}

// ============================================================
// Voice Orb
// ============================================================
@Composable
private fun VoiceOrb(
    isRecording     : Boolean,
    isProcessing    : Boolean,
    onStartRecording: () -> Unit,
    onStopRecording : () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isRecording || isProcessing) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (isRecording) 700 else 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    val outerGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = if (isRecording) 0.9f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val orbColor1 = if (isRecording) RedError else if (isProcessing) Violet500 else Navy700
    val orbColor2 = if (isRecording) Coral500 else if (isProcessing) Violet300 else Navy600

    Box(contentAlignment = Alignment.Center) {
        // Outer glow ring (only when recording)
        if (isRecording) {
            Box(
                Modifier
                    .size(148.dp)
                    .clip(CircleShape)
                    .background(RedError.copy(alpha = outerGlow * 0.2f))
            )
        }

        // Main orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(orbColor1, orbColor2)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) {
                    if (isRecording) onStopRecording() else if (!isProcessing) onStartRecording()
                }
        ) {
            when {
                isProcessing -> CircularProgressIndicator(
                    color       = White,
                    strokeWidth = 3.dp,
                    modifier    = Modifier.size(40.dp)
                )
                isRecording  -> Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint     = White,
                    modifier = Modifier.size(40.dp)
                )
                else         -> Icon(
                    Icons.Default.Mic,
                    contentDescription = "Speak",
                    tint     = Grey300,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
