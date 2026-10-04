package com.voicereminder.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicereminder.network.MobileCommandResponse
import com.voicereminder.ui.theme.Coral500
import com.voicereminder.ui.theme.Grey300
import com.voicereminder.ui.theme.Grey500
import com.voicereminder.ui.theme.GreenSuccess
import com.voicereminder.ui.theme.Navy600
import com.voicereminder.ui.theme.Navy700
import com.voicereminder.ui.theme.Navy800
import com.voicereminder.ui.theme.Navy900
import com.voicereminder.ui.theme.OrangeWarning
import com.voicereminder.ui.theme.RedError
import com.voicereminder.ui.theme.Violet400
import com.voicereminder.ui.theme.Violet500
import com.voicereminder.ui.theme.White
import com.voicereminder.viewmodel.RecordingState
import java.util.Locale

data class QuickCommand(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun UniversalCommandBar(
    recordingState: RecordingState,
    liveTranscript: String,
    voiceRmsDb: Float,
    recordingStartedAtMs: Long?,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmitText: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    onQuickCommand: (String) -> Unit,
    quickCommands: List<QuickCommand> = defaultQuickCommands(),
    successResponse: MobileCommandResponse? = null,
    onSuccessPrimaryAction: (() -> Unit)? = null,
    successPrimaryLabel: String = "View",
    onSuccessSecondaryAction: (() -> Unit)? = null,
    successSecondaryLabel: String = "Done",
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val isActive = recordingState != RecordingState.Idle || value.isNotBlank() || expanded
    val targetHeight = when (recordingState) {
        RecordingState.Idle -> if (isActive) 196.dp else 88.dp
        RecordingState.Activating -> 244.dp
        RecordingState.Recording -> 270.dp
        RecordingState.Processing -> 298.dp
        is RecordingState.Success -> 176.dp
        is RecordingState.Error -> 184.dp
    }
    val surfaceHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "universalCommandBarHeight",
    )
    val borderColor = when (recordingState) {
        RecordingState.Idle -> Navy600.copy(alpha = 0.72f)
        RecordingState.Activating, RecordingState.Recording -> OrangeWarning.copy(alpha = 0.38f)
        RecordingState.Processing -> Violet400.copy(alpha = 0.38f)
        is RecordingState.Success -> GreenSuccess.copy(alpha = 0.32f)
        is RecordingState.Error -> RedError.copy(alpha = 0.32f)
    }

    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, shape, clip = false)
            .border(1.dp, borderColor, shape),
        shape = shape,
        color = Navy800.copy(alpha = 0.97f),
    ) {
        Column(
            modifier = Modifier
                .height(surfaceHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AnimatedContent(targetState = recordingState, label = "commandBarState") { state ->
                when (state) {
                    RecordingState.Idle -> IdleCommandBar(
                        value = value,
                        onValueChange = {
                            onValueChange(it)
                            if (it.isNotBlank()) expanded = true
                        },
                        onSend = {
                            val trimmed = value.trim()
                            if (trimmed.isNotEmpty()) {
                                onSubmitText(trimmed)
                                onValueChange("")
                                expanded = false
                            }
                        },
                        onVoiceTap = {
                            expanded = true
                            onStartRecording()
                        },
                        onAttachFile = onAttachFile,
                        onCaptureDocument = onCaptureDocument,
                        onOpenCamera = onOpenCamera,
                        quickCommands = quickCommands,
                        onQuickCommand = onQuickCommand,
                    )

                    RecordingState.Activating -> ActivationCommandBar(
                        liveTranscript = liveTranscript,
                        onCancel = {
                            expanded = false
                            onCancelRecording()
                        },
                    )

                    RecordingState.Recording -> RecordingCommandBar(
                        liveTranscript = liveTranscript,
                        voiceRmsDb = voiceRmsDb,
                        recordingStartedAtMs = recordingStartedAtMs,
                        onCancel = {
                            expanded = false
                            onCancelRecording()
                        },
                        onStop = onStopRecording,
                    )

                    RecordingState.Processing -> ProcessingCommandBar(liveTranscript = liveTranscript)

                    is RecordingState.Success -> SuccessCommandBar(
                        response = successResponse ?: state.response,
                        onPrimaryAction = onSuccessPrimaryAction,
                        primaryLabel = successPrimaryLabel,
                        onSecondaryAction = {
                            expanded = false
                            onSuccessSecondaryAction?.invoke() ?: onDismissState()
                        },
                        secondaryLabel = successSecondaryLabel,
                    )

                    is RecordingState.Error -> ErrorCommandBar(
                        message = state.message,
                        onRetry = {
                            expanded = true
                            onStartRecording()
                        },
                        onDismiss = {
                            expanded = false
                            onDismissState()
                        },
                    )
                }
            }

            CommandBarGrip()
        }
    }
}

@Composable
private fun IdleCommandBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceTap: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    quickCommands: List<QuickCommand>,
    onQuickCommand: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Navy700.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, Navy600.copy(alpha = 0.55f)),
                onClick = onVoiceTap,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VoiceAffordance(rmsDb = 0.18f, accent = Violet500)
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Violet400, modifier = Modifier.size(16.dp))
                    Text("Speak", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CommandPillIcon(Icons.Default.AttachFile, onAttachFile)
                CommandPillIcon(Icons.Default.Description, onCaptureDocument)
                CommandPillIcon(Icons.Default.CameraAlt, onOpenCamera)
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask or speak anything...", color = Grey500) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            trailingIcon = {
                CommandPillIcon(Icons.Default.Keyboard, onSend, tint = Violet400)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet400,
                unfocusedBorderColor = Navy600,
                focusedTextColor = White,
                unfocusedTextColor = White,
                cursorColor = Violet400,
                focusedContainerColor = Navy800,
                unfocusedContainerColor = Navy800,
            ),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickCommands) { command ->
                FilterChip(
                    selected = false,
                    onClick = { onQuickCommand(command.label) },
                    label = { Text(command.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(command.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Navy700.copy(alpha = 0.72f),
                        labelColor = White,
                        iconColor = Violet400,
                    ),
                    border = BorderStroke(1.dp, Navy600.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

@Composable
private fun ActivationCommandBar(
    liveTranscript: String,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(OrangeWarning.copy(alpha = 0.12f))
                        .border(1.dp, OrangeWarning.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(12.dp))
                }
                Column {
                    Text("Listening", color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (liveTranscript.isBlank()) "Wake word detected." else liveTranscript,
                        color = Grey500,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CommandTextButton("Cancel", onCancel)
        }
        VoiceRibbon(rmsDb = 0.18f, accent = OrangeWarning)
    }
}

@Composable
private fun RecordingCommandBar(
    liveTranscript: String,
    voiceRmsDb: Float,
    recordingStartedAtMs: Long?,
    onCancel: () -> Unit,
    onStop: () -> Unit,
) {
    val elapsed = rememberRecordingTicker(recordingStartedAtMs)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(OrangeWarning.copy(alpha = 0.12f))
                        .border(1.dp, OrangeWarning.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(12.dp))
                }
                Column {
                    Text("Recording", color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(elapsed, color = Grey500, fontSize = 12.sp)
                }
            }
            CommandTextButton("Cancel", onCancel)
        }
        VoiceRibbon(rmsDb = voiceRmsDb, accent = OrangeWarning)
        Text(
            text = if (liveTranscript.isBlank()) "Speak naturally..." else liveTranscript,
            color = White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OrangeWarning))
            Text("Live voice input", color = Grey500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            CommandTextButton("Stop", onStop)
        }
    }
}

@Composable
private fun ProcessingCommandBar(liveTranscript: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Executing", color = White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("Turning your request into an action", color = Grey500, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Navy700.copy(alpha = 0.72f))
                    .border(1.dp, Violet400.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("Processing", color = Violet400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (liveTranscript.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Navy700.copy(alpha = 0.5f))
                    .border(1.dp, Navy600.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = liveTranscript.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    color = White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ProcessingTimeline()
        }
    }
}

@Composable
private fun ProcessingTimeline() {
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "processingPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "processingPulseAlpha",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TimelineLine("Understanding request", CmdBarTimelineState.DONE, pulse)
        TimelineLine("Extracting date and time", CmdBarTimelineState.DONE, pulse)
        TimelineLine("Checking existing items", CmdBarTimelineState.ACTIVE, pulse)
        TimelineLine("Creating reminder or task", CmdBarTimelineState.PENDING, pulse)
    }
}

private enum class CmdBarTimelineState { DONE, ACTIVE, PENDING }

@Composable
private fun TimelineLine(label: String, state: CmdBarTimelineState, pulse: Float) {
    val color = when (state) {
        CmdBarTimelineState.DONE -> GreenSuccess
        CmdBarTimelineState.ACTIVE -> Violet400
        CmdBarTimelineState.PENDING -> Grey500
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    when (state) {
                        CmdBarTimelineState.DONE -> color.copy(alpha = 0.18f)
                        CmdBarTimelineState.ACTIVE -> color.copy(alpha = 0.16f)
                        CmdBarTimelineState.PENDING -> Navy600.copy(alpha = 0.25f)
                    }
                )
                .border(1.dp, color.copy(alpha = if (state == CmdBarTimelineState.ACTIVE) 0.45f else 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                CmdBarTimelineState.DONE -> Icon(Icons.Default.Check, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
                CmdBarTimelineState.ACTIVE -> Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color.copy(alpha = pulse)))
                CmdBarTimelineState.PENDING -> Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color.copy(alpha = 0.55f)))
            }
        }

        Text(
            text = label,
            color = if (state == CmdBarTimelineState.PENDING) Grey500 else White,
            fontSize = 13.sp,
            fontWeight = if (state == CmdBarTimelineState.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
        )

        if (state == CmdBarTimelineState.ACTIVE) {
            Spacer(Modifier.weight(1f))
            Text("Working", color = Violet400.copy(alpha = pulse), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SuccessCommandBar(
    response: MobileCommandResponse,
    onPrimaryAction: (() -> Unit)?,
    primaryLabel: String,
    onSecondaryAction: () -> Unit,
    secondaryLabel: String,
) {
    val title = response.task?.takeIf { it.isNotBlank() } ?: "Request completed"
    val subtitle = response.scheduledAt?.takeIf { it.isNotBlank() } ?: response.replyText.ifBlank { "All set." }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(GreenSuccess.copy(alpha = 0.12f))
                    .border(1.dp, GreenSuccess.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Reminder created", color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(title, color = Grey500, fontSize = 12.sp)
                Text(subtitle, color = Grey500, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onPrimaryAction != null) {
                CommandTextButton(primaryLabel, onPrimaryAction)
            }
            Surface(onClick = onSecondaryAction, shape = RoundedCornerShape(999.dp), color = Violet400) {
                Text(
                    secondaryLabel,
                    color = Navy900,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorCommandBar(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(RedError.copy(alpha = 0.12f))
                    .border(1.dp, RedError.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = RedError, modifier = Modifier.size(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Needs attention", color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(message, color = Grey500, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CommandTextButton("Try again", onRetry)
            CommandTextButton("Dismiss", onDismiss)
        }
    }
}

@Composable
private fun CommandTextButton(text: String, onClick: () -> Unit) {
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
private fun CommandPillIcon(icon: ImageVector, onClick: () -> Unit, tint: Color = Grey300) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Navy700.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Navy600.copy(alpha = 0.5f)),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(10.dp).size(18.dp))
    }
}

@Composable
private fun CommandBarGrip() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Navy600),
        )
    }
}

@Composable
private fun VoiceRibbon(rmsDb: Float, accent: Color) {
    val bars = listOf(0.20f, 0.30f, 0.42f, 0.56f, 0.46f, 0.32f, 0.22f)
    val level = ((rmsDb + 2f) / 8f).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Navy700.copy(alpha = 0.55f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bars.forEachIndexed { index, base ->
            val phase = ((index - 3).toFloat() * 0.08f) + level
            val animated by animateFloatAsState(
                targetValue = (base + phase).coerceIn(0.12f, 0.92f),
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "commandBarRibbon$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((animated * 11f).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.24f + (animated * 0.24f))),
            )
        }
    }
}

@Composable
private fun VoiceAffordance(rmsDb: Float, accent: Color) {
    val pulse = ((rmsDb + 2f) / 8f).coerceIn(0.2f, 1f)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { index ->
            val height = when (index) {
                0 -> 5.dp
                1 -> 9.dp
                2 -> 13.dp
                else -> 8.dp
            }
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = height + (pulse * (index + 2)).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.36f)),
            )
        }
    }
}

@Composable
private fun rememberRecordingTicker(startedAtMs: Long?): String {
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(startedAtMs) {
        while (startedAtMs != null) {
            kotlinx.coroutines.delay(1000)
            tick++
        }
    }
    val elapsedSeconds = if (startedAtMs == null) 0L else ((System.currentTimeMillis() - startedAtMs) / 1000L).coerceAtLeast(0L)
    val minutes = elapsedSeconds / 60L
    val seconds = elapsedSeconds % 60L
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun defaultQuickCommands(): List<QuickCommand> = listOf(
    QuickCommand("Remind me tomorrow 5 PM", Icons.Default.Search),
    QuickCommand("Add a task", Icons.Default.Check),
    QuickCommand("Capture a note", Icons.Default.Description),
    QuickCommand("Attach a file", Icons.Default.AttachFile),
    QuickCommand("Take a photo", Icons.Default.CameraAlt),
)
