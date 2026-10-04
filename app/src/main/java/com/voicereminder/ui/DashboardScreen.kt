package com.voicereminder.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicereminder.data.ReminderEntity
import com.voicereminder.network.MobileCommandResponse
import com.voicereminder.viewmodel.ChatMessage
import com.voicereminder.viewmodel.HomeUiState
import com.voicereminder.viewmodel.MessageSender
import com.voicereminder.viewmodel.RecordingState
import java.text.SimpleDateFormat
import java.util.*

private val BgColor = Color(0xFFF9F9FA)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1D1D1F)
private val TextSecondary = Color(0xFF86868B)
private val AccentColor = Color(0xFF5E5CE6)
private val AccentLight = Color(0xFFEAEAFF)
private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun DashboardScreen(
    uiState: HomeUiState,
    reminders: List<ReminderEntity>,
    onToggleReminder: (ReminderEntity) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToResearch: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSubmitText: (String) -> Unit,
    hasOverlayPermission: Boolean = true,
    onRequestOverlayPermission: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            HomeHeader()
            if (!hasOverlayPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF3F0FF),
                    border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AccentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Siri Floating Assistant", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text("Allow display over other apps to talk to assistant anywhere!", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onRequestOverlayPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            TodaySummary(reminders = reminders)
            Spacer(modifier = Modifier.height(32.dp))
            VoiceExperienceSection(
                recordingState = uiState.recordingState,
                liveTranscript = uiState.liveTranscript,
                recordingStartedAtMs = uiState.recordingStartedAtMs,
                voiceRmsDb = uiState.voiceRmsDb,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onCancelRecording = onCancelRecording,
                onDismiss = onDismiss
            )
            Spacer(modifier = Modifier.height(40.dp))
            TodaysPrioritiesSection(reminders, onToggleReminder)
            Spacer(modifier = Modifier.height(32.dp))
            RecentActivitySection(uiState.messages)
            Spacer(modifier = Modifier.height(100.dp))
        }


    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good morning, Vansh",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Here's what needs your attention today.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        Surface(
            shape = CircleShape,
            color = AccentLight,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("V", color = AccentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun TodaySummary(reminders: List<ReminderEntity>) {
    val pending = reminders.count { !it.isCompleted }
    Column {
        Text("TODAY", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(count = pending, label = "Reminders")
            Divider(modifier = Modifier.height(32.dp).width(1.dp), color = DividerColor)
            SummaryItem(count = 0, label = "Tasks")
            Divider(modifier = Modifier.height(32.dp).width(1.dp), color = DividerColor)
            SummaryItem(count = 0, label = "Notes")
        }
    }
}

@Composable
private fun SummaryItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun VoiceExperienceSection(
    recordingState: RecordingState,
    liveTranscript: String,
    recordingStartedAtMs: Long?,
    voiceRmsDb: Float,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(targetState = recordingState, label = "voice_state") { state ->
            when (state) {
                RecordingState.Idle -> VoiceIdle(onStartRecording)
                RecordingState.Activating, RecordingState.Recording -> VoiceRecording(
                    liveTranscript, voiceRmsDb, recordingStartedAtMs, onStopRecording, onCancelRecording
                )
                RecordingState.Processing -> VoiceProcessing(liveTranscript)
                is RecordingState.Success -> VoiceSuccess(state.response, onDismiss)
                is RecordingState.Error -> VoiceError(state.message, onDismiss)
            }
        }
    }
}

@Composable
private fun VoiceIdle(onStartRecording: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Active indicator badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AccentLight,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Always Listening: Say \"Hey\" or Tap",
                    color = AccentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "What can I help you remember?",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(20.dp))

        Box(contentAlignment = Alignment.Center) {
            // Animated breathing halo
            Box(
                modifier = Modifier
                    .size(76.dp * haloScale)
                    .clip(CircleShape)
                    .background(AccentColor.copy(alpha = haloAlpha))
            )
            // Primary interactive mic button
            Surface(
                shape = CircleShape,
                color = AccentColor,
                onClick = onStartRecording,
                modifier = Modifier.size(72.dp),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic", tint = SurfaceColor, modifier = Modifier.size(32.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Tap to speak directly",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VoiceRecording(
    liveTranscript: String,
    voiceRmsDb: Float,
    recordingStartedAtMs: Long?,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Listening...", color = AccentColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
            val level = ((voiceRmsDb + 2f) / 8f).coerceIn(0f, 1f)
            listOf(0.4f, 0.6f, 0.8f, 1f, 0.7f, 0.5f, 0.3f).forEachIndexed { index, base ->
                val animated = animateFloatAsState(targetValue = base * level + 0.2f, label = "wave_$index")
                Box(modifier = Modifier.width(4.dp).height((animated.value * 32).dp).clip(CircleShape).background(AccentColor))
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Text(
            text = rememberRecordingTicker(recordingStartedAtMs),
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (liveTranscript.isBlank()) "Speak naturally..." else liveTranscript,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary, fontSize = 16.sp)
            }
            Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = AccentColor)) {
                Text("Done", color = SurfaceColor, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun VoiceProcessing(liveTranscript: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("I understood:", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "\"$liveTranscript\"",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(16.dp))
        Text("Processing request...", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun VoiceSuccess(response: MobileCommandResponse, onDismiss: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Task completed", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(response.replyText, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AccentColor)) {
            Text("Done")
        }
    }
}

@Composable
private fun VoiceError(message: String, onDismiss: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Something went wrong", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AccentColor)) {
            Text("Okay")
        }
    }
}

@Composable
private fun TodaysPrioritiesSection(reminders: List<ReminderEntity>, onToggle: (ReminderEntity) -> Unit) {
    val todayReminders = reminders.filter { !it.isCompleted }.take(4)
    Column {
        Text(
            text = "Today's priorities",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))
        
        if (todayReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceColor, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("You're all caught up", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    todayReminders.forEachIndexed { index, reminder ->
                        PriorityRow(reminder, onToggle)
                        if (index < todayReminders.size - 1) {
                            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityRow(reminder: ReminderEntity, onToggle: (ReminderEntity) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(reminder) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (reminder.isCompleted) AccentColor else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.task,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            val timeString = formatTime(reminder.scheduledAt)
            Text(
                text = "Today · $timeString",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RecentActivitySection(messages: List<ChatMessage>) {
    val activities = messages.filter { it.sender == MessageSender.ASSISTANT && !it.isError }.takeLast(3).reversed()
    Column {
        Text(
            text = "Recent activity",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        if (activities.isEmpty()) {
            Text("No recent activity", color = TextSecondary, fontSize = 14.sp)
        } else {
            activities.forEach { msg ->
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentColor))
                    Spacer(Modifier.width(12.dp))
                    Text(msg.text, color = TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}



@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) AccentColor else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) AccentColor else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
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
    val totalSeconds = (elapsed / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}
