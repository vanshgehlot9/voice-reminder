package com.voicereminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicereminder.data.ReminderEntity
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
import com.voicereminder.ui.theme.White
import com.voicereminder.viewmodel.HomeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    uiState: HomeUiState,
    onBack: () -> Unit,
    onContinueInteraction: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitText: (String) -> Unit,
) {
    val history = remember {
        listOf(
            ConversationTimelineEntry(
                request = "Set a reminder for tomorrow at 5 PM",
                action = "Detected reminder creation",
                result = "Reminder created · Tomorrow · 5:00 PM",
                dateLabel = "Today",
            ),
            ConversationTimelineEntry(
                request = "Summarize my notes from the client call",
                action = "Detected note summarization",
                result = "Saved summary to Notes · 6 bullets",
                dateLabel = "Today",
            ),
            ConversationTimelineEntry(
                request = "Find the latest research on battery health",
                action = "Detected research workflow",
                result = "Opened research draft · 4 sources collected",
                dateLabel = "Yesterday",
            ),
        )
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var dateFilter by rememberSaveable { mutableStateOf("All") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var commandDraft by rememberSaveable { mutableStateOf("") }

    val filtered = history.filter { entry ->
        (searchQuery.isBlank() || entry.request.contains(searchQuery, ignoreCase = true) || entry.result.contains(searchQuery, ignoreCase = true)) &&
            (dateFilter == "All" || entry.dateLabel == dateFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversation History", color = White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
            )
        },
        containerColor = Navy900,
        bottomBar = {
            UniversalCommandBar(
                recordingState = uiState.recordingState,
                liveTranscript = uiState.liveTranscript,
                voiceRmsDb = uiState.voiceRmsDb,
                recordingStartedAtMs = uiState.recordingStartedAtMs,
                value = commandDraft,
                onValueChange = { commandDraft = it },
                onSubmitText = {
                    onSubmitText(it)
                    commandDraft = ""
                },
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onCancelRecording = onCancelRecording,
                onDismissState = onDismissState,
                onAttachFile = onAttachFile,
                onCaptureDocument = onCaptureDocument,
                onOpenCamera = onOpenCamera,
                onQuickCommand = {
                    commandDraft = it
                    onContinueInteraction(it)
                },
                successResponse = null,
                onSuccessPrimaryAction = null,
                onSuccessSecondaryAction = onDismissState,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Navy800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.5f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Activity timeline", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Each interaction shows the request, the detected action, and the result.", color = Grey500, fontSize = 12.sp)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search history", color = Grey500) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Grey500) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedContainerColor = Navy700,
                                unfocusedContainerColor = Navy700,
                                focusedBorderColor = Violet400,
                                unfocusedBorderColor = Navy600,
                            ),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Today", "Yesterday").forEach { filter ->
                                FilterChip(
                                    selected = dateFilter == filter,
                                    onClick = { dateFilter = filter },
                                    label = { Text(filter) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Violet400,
                                        selectedLabelColor = Navy900,
                                        containerColor = Navy700,
                                        labelColor = White,
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.5f)),
                                )
                            }
                        }
                    }
                }
            }

            items(filtered) { entry ->
                val isSelected = selected == entry.request
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isSelected) Navy700.copy(alpha = 0.92f) else Navy800.copy(alpha = 0.72f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Violet400.copy(alpha = 0.45f) else Navy600.copy(alpha = 0.45f)),
                    modifier = Modifier.clickable { selected = entry.request },
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Coral500),
                            )
                            Text(entry.dateLabel, color = Grey500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("Continue", color = Violet400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(entry.request, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(entry.action, color = Grey300, fontSize = 12.sp)
                        HorizontalDivider(color = Navy600.copy(alpha = 0.45f))
                        Text(entry.result, color = White, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                onClick = { onContinueInteraction(entry.request) },
                                shape = RoundedCornerShape(999.dp),
                                color = Violet400,
                            ) {
                                Text("Reopen", color = Navy900, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                            }
                            Surface(
                                onClick = { onContinueInteraction(entry.request) },
                                shape = RoundedCornerShape(999.dp),
                                color = Navy700,
                            ) {
                                Text("Continue", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ConversationTimelineEntry(
    val request: String,
    val action: String,
    val result: String,
    val dateLabel: String,
)


