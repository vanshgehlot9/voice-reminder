package com.voicereminder.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicereminder.data.ReminderEntity
import com.voicereminder.viewmodel.HomeUiState
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
private val CompletedColor = Color(0xFF34C759)
private val ErrorColor = Color(0xFFFF3B30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    reminders: List<ReminderEntity>,
    uiState: HomeUiState,
    onBack: () -> Unit,
    onOpenDetail: (ReminderEntity) -> Unit,
    onComplete: (ReminderEntity) -> Unit,
    onSnooze: (ReminderEntity) -> Unit,
    onReschedule: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitText: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf("Today") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isAddSheetOpen by rememberSaveable { mutableStateOf(false) }
    var selectedReminderForOptions by remember { mutableStateOf<ReminderEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.recordingState) {
        if (uiState.recordingState is RecordingState.Success) {
            snackbarHostState.showSnackbar("Reminder created successfully", duration = SnackbarDuration.Short)
            onDismissState()
            isAddSheetOpen = false
        } else if (uiState.recordingState is RecordingState.Error) {
            val msg = (uiState.recordingState as RecordingState.Error).message
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            onDismissState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RemindersTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchToggle = { 
                    isSearchActive = !isSearchActive 
                    if (!isSearchActive) searchQuery = "" 
                },
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddSheetOpen = true },
                containerColor = AccentColor,
                contentColor = SurfaceColor,
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add reminder", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = BgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isSearchActive) {
                ReminderFilter(selectedTab) { selectedTab = it }
            }
            
            val filteredReminders = if (isSearchActive && searchQuery.isNotBlank()) {
                reminders.filter { it.task.contains(searchQuery, ignoreCase = true) }
            } else reminders
            
            if (isSearchActive && searchQuery.isNotBlank()) {
                if (filteredReminders.isEmpty()) {
                    ReminderEmptyState(
                        title = "No reminders found",
                        message = "Try a different search term.",
                        buttonText = null,
                        onAction = {}
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(filteredReminders, key = { it.id }) { reminder ->
                            ReminderRow(reminder, onToggle = onComplete, onOpenOptions = { selectedReminderForOptions = it })
                            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            } else if (isSearchActive) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("Search your reminders", color = TextSecondary, fontSize = 14.sp)
                 }
            } else {
                when (selectedTab) {
                    "Today" -> TodaySection(filteredReminders, onComplete) { selectedReminderForOptions = it }
                    "Upcoming" -> UpcomingSection(filteredReminders, onComplete) { selectedReminderForOptions = it }
                    "Completed" -> CompletedSection(filteredReminders, onComplete) { selectedReminderForOptions = it }
                }
            }
        }
    }

    if (isAddSheetOpen) {
        AddReminderSheet(
            onDismiss = { isAddSheetOpen = false },
            uiState = uiState,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onCancelRecording = onCancelRecording,
            onSubmitText = onSubmitText
        )
    }

    selectedReminderForOptions?.let { reminder ->
        ReminderActionSheet(
            reminder = reminder,
            onDismiss = { selectedReminderForOptions = null },
            onEdit = onOpenDetail,
            onReschedule = onReschedule,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onBack: () -> Unit
) {
    if (isSearchActive) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search reminders...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = {
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
        )
    } else {
        TopAppBar(
            title = { Text("Reminders", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            actions = {
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
        )
    }
}

@Composable
private fun ReminderFilter(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("Today", "Upcoming", "Completed")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) SurfaceColor else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TodaySection(reminders: List<ReminderEntity>, onToggle: (ReminderEntity) -> Unit, onOpenOptions: (ReminderEntity) -> Unit) {
    val todayReminders = reminders.filter { !it.isCompleted && isToday(it.scheduledAt) }.sortedBy { it.scheduledAt }
    if (todayReminders.isEmpty()) {
        ReminderEmptyState("No reminders today", "You're all caught up.", "Create reminder", onAction = null)
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Text("Today · ${todayReminders.size}", modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            items(todayReminders, key = { it.id }) { reminder ->
                ReminderRow(reminder, onToggle, onOpenOptions)
                Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun UpcomingSection(reminders: List<ReminderEntity>, onToggle: (ReminderEntity) -> Unit, onOpenOptions: (ReminderEntity) -> Unit) {
    val upcomingReminders = reminders.filter { !it.isCompleted && !isToday(it.scheduledAt) }.sortedBy { it.scheduledAt }
    if (upcomingReminders.isEmpty()) {
        ReminderEmptyState("Nothing scheduled", "Your upcoming reminders will appear here.", "Add reminder", onAction = null)
    } else {
        val grouped = upcomingReminders.groupBy { formatReminderDate(it.scheduledAt) }
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            grouped.forEach { (dateStr, list) ->
                item {
                    Text(dateStr, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                items(list, key = { it.id }) { reminder ->
                    ReminderRow(reminder, onToggle, onOpenOptions)
                    Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun CompletedSection(reminders: List<ReminderEntity>, onToggle: (ReminderEntity) -> Unit, onOpenOptions: (ReminderEntity) -> Unit) {
    val completedReminders = reminders.filter { it.isCompleted }.sortedByDescending { it.scheduledAt }
    if (completedReminders.isEmpty()) {
        ReminderEmptyState("No completed reminders", "Completed reminders will appear here.", null, onAction = null)
    } else {
        val grouped = completedReminders.groupBy { if (isToday(it.scheduledAt)) "COMPLETED TODAY" else "COMPLETED PREVIOUSLY" }
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            grouped.forEach { (dateStr, list) ->
                item {
                    Text(dateStr, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                items(list, key = { it.id }) { reminder ->
                    ReminderRow(reminder, onToggle, onOpenOptions)
                    Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderEntity,
    onToggle: (ReminderEntity) -> Unit,
    onOpenOptions: (ReminderEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(reminder) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = "Toggle completion",
            tint = if (reminder.isCompleted) CompletedColor else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.task,
                color = if (reminder.isCompleted) TextSecondary else TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val timeText = formatReminderTime(reminder.scheduledAt)
            Text(
                text = timeText,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        
        IconButton(onClick = { onOpenOptions(reminder) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
        }
    }
}

@Composable
private fun ReminderEmptyState(title: String, message: String, buttonText: String?, onAction: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DividerColor, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderSheet(
    onDismiss: () -> Unit,
    uiState: HomeUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onSubmitText: (String) -> Unit
) {
    var textDraft by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create reminder", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (uiState.recordingState != RecordingState.Idle) {
                VoiceRecordingStateIndicator(
                    recordingState = uiState.recordingState,
                    liveTranscript = uiState.liveTranscript,
                    onStop = onStopRecording,
                    onCancel = onCancelRecording
                )
            } else {
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { textDraft = it },
                    placeholder = { Text("Type a reminder...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = { 
                                if (textDraft.isNotBlank()) {
                                    onSubmitText(textDraft)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = AccentColor)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentColor,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = BgColor,
                        unfocusedContainerColor = BgColor
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("or", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Speak a reminder", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = CircleShape,
                    color = AccentColor,
                    onClick = onStartRecording,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mic, contentDescription = "Mic", tint = SurfaceColor, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceRecordingStateIndicator(
    recordingState: RecordingState,
    liveTranscript: String,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        when (recordingState) {
            RecordingState.Activating, RecordingState.Recording -> {
                Text("Listening...", color = AccentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(if (liveTranscript.isBlank()) "Speak naturally..." else liveTranscript, color = TextPrimary, modifier = Modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onCancel) { Text("Cancel", color = TextSecondary) }
                    Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = AccentColor)) { Text("Done") }
                }
            }
            RecordingState.Processing -> {
                CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(32.dp))
                Text("Processing request...", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
            }
            is RecordingState.Success -> {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CompletedColor, modifier = Modifier.size(48.dp))
                Text(recordingState.response.replyText, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
            }
            is RecordingState.Error -> {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(48.dp))
                Text(recordingState.message, color = ErrorColor, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderActionSheet(
    reminder: ReminderEntity,
    onDismiss: () -> Unit,
    onEdit: (ReminderEntity) -> Unit,
    onReschedule: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceColor) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = reminder.task,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            Divider(color = DividerColor)
            
            ActionSheetRow(icon = Icons.Default.Edit, text = "Edit", onClick = { onEdit(reminder); onDismiss() })
            ActionSheetRow(icon = Icons.Default.Event, text = "Reschedule", onClick = { onReschedule(reminder); onDismiss() })
            ActionSheetRow(icon = Icons.Default.Delete, text = "Delete", tint = ErrorColor, onClick = { onDelete(reminder); onDismiss() })
        }
    }
}

@Composable
private fun ActionSheetRow(icon: ImageVector, text: String, tint: Color = TextPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminder: ReminderEntity,
    onBack: () -> Unit,
    onSave: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by rememberSaveable(reminder.id) { mutableStateOf(reminder.task) }
    var date by rememberSaveable(reminder.id) { mutableStateOf(reminder.scheduledAtIso.takeIf { it.isNotBlank() } ?: formatReminderTime(reminder.scheduledAt)) }
    var notes by rememberSaveable(reminder.id) { mutableStateOf(reminder.transcript) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (reminder.scheduledAt == 0L) "Document Details" else "Edit Reminder", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    val exportText = notes.ifBlank { reminder.replyText }
                    if (exportText.isNotBlank()) {
                        IconButton(onClick = {
                            val file = com.voicereminder.util.PdfGenerator.generateDocumentPdf(context, title, exportText)
                            if (file != null) {
                                com.voicereminder.util.PdfGenerator.openPdf(context, file)
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = AccentColor)
                        }
                        IconButton(onClick = {
                            val file = com.voicereminder.util.PdfGenerator.generateDocumentPdf(context, title, exportText)
                            if (file != null) {
                                com.voicereminder.util.PdfGenerator.sharePdf(context, file)
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = AccentColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor),
            )
        },
        containerColor = BgColor,
        bottomBar = {
            Surface(color = SurfaceColor, shadowElevation = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDelete(reminder) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor.copy(alpha = 0.5f))
                    ) {
                        Text("Delete")
                    }
                    Button(
                        onClick = {
                            onSave(reminder.copy(task = title, scheduledAtIso = date, transcript = notes))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailField("Reminder title", title) { title = it }
            DetailField("Date & Time", date) { date = it }
            DetailField("Notes", notes, minLines = 4) { notes = it }
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    placeholder: String = "",
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentColor,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor
            )
        )
    }
}

private fun isToday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatReminderTime(millis: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatReminderDate(millis: Long): String {
    val formatter = SimpleDateFormat("EEEE · MMM d", Locale.getDefault())
    return formatter.format(Date(millis)).uppercase()
}
