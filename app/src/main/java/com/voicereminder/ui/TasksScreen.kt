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
fun TasksScreen(
    tasks: List<ReminderEntity>,
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
    onSubmitText: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf("Today") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isAddSheetOpen by rememberSaveable { mutableStateOf(false) }
    var selectedTaskForOptions by remember { mutableStateOf<ReminderEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.recordingState) {
        if (uiState.recordingState is RecordingState.Success) {
            snackbarHostState.showSnackbar("Task created successfully", duration = SnackbarDuration.Short)
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
            TasksTopBar(
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
                Text("New task", fontWeight = FontWeight.SemiBold)
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
                TaskFilter(selectedTab) { selectedTab = it }
            }
            
            val filteredTasks = if (isSearchActive && searchQuery.isNotBlank()) {
                tasks.filter { it.task.contains(searchQuery, ignoreCase = true) }
            } else tasks
            
            if (isSearchActive && searchQuery.isNotBlank()) {
                if (filteredTasks.isEmpty()) {
                    TaskEmptyState(
                        title = "No tasks found",
                        message = "Try a different search term.",
                        buttonText = null,
                        onAction = null
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(filteredTasks, key = { it.id }) { task ->
                            TaskRow(task, onToggle = onComplete, onOpenOptions = { selectedTaskForOptions = it })
                            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            } else if (isSearchActive) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("Search your tasks", color = TextSecondary, fontSize = 14.sp)
                 }
            } else {
                when (selectedTab) {
                    "Today" -> TaskTodaySection(filteredTasks, onComplete) { selectedTaskForOptions = it }
                    "Upcoming" -> TaskUpcomingSection(filteredTasks, onComplete) { selectedTaskForOptions = it }
                }
            }
        }
    }

    if (isAddSheetOpen) {
        AddTaskSheet(
            onDismiss = { isAddSheetOpen = false },
            uiState = uiState,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onCancelRecording = onCancelRecording,
            onSubmitText = onSubmitText
        )
    }

    selectedTaskForOptions?.let { task ->
        TaskActionSheet(
            task = task,
            onDismiss = { selectedTaskForOptions = null },
            onEdit = onOpenDetail,
            onReschedule = onReschedule,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksTopBar(
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
                    placeholder = { Text("Search tasks...") },
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
            title = { Text("Tasks", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
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
private fun TaskFilter(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("Today", "Upcoming")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
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
private fun TaskTodaySection(tasks: List<ReminderEntity>, onToggle: (ReminderEntity) -> Unit, onOpenOptions: (ReminderEntity) -> Unit) {
    val todayTasks = tasks.filter { !it.isCompleted && isToday(it.scheduledAt) }.sortedBy { it.scheduledAt }
    if (todayTasks.isEmpty()) {
        TaskEmptyState("No tasks for today", "You're all caught up.", "Create task", onAction = null)
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Text("TODAY · ${todayTasks.size} TASKS", modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            items(todayTasks, key = { it.id }) { task ->
                TaskRow(task, onToggle, onOpenOptions)
                Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun TaskUpcomingSection(tasks: List<ReminderEntity>, onToggle: (ReminderEntity) -> Unit, onOpenOptions: (ReminderEntity) -> Unit) {
    val upcomingTasks = tasks.filter { !it.isCompleted && !isToday(it.scheduledAt) }.sortedBy { it.scheduledAt }
    if (upcomingTasks.isEmpty()) {
        TaskEmptyState("No upcoming tasks", "Your schedule is clear.", "Add task", onAction = null)
    } else {
        val grouped = upcomingTasks.groupBy { formatTaskDate(it.scheduledAt) }
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            grouped.forEach { (dateStr, list) ->
                item {
                    Text(dateStr, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                items(list, key = { it.id }) { task ->
                    TaskRow(task, onToggle, onOpenOptions)
                    Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: ReminderEntity,
    onToggle: (ReminderEntity) -> Unit,
    onOpenOptions: (ReminderEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(task) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = "Toggle completion",
            tint = if (task.isCompleted) CompletedColor else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.task,
                color = if (task.isCompleted) TextSecondary else TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val timeText = formatTaskTime(task.scheduledAt)
            Text(
                text = timeText,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        
        IconButton(onClick = { onOpenOptions(task) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
        }
    }
}

@Composable
private fun TaskEmptyState(title: String, message: String, buttonText: String?, onAction: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.TaskAlt, contentDescription = null, tint = DividerColor, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(
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
            Text("Create task", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (uiState.recordingState != RecordingState.Idle) {
                TaskVoiceRecordingStateIndicator(
                    recordingState = uiState.recordingState,
                    liveTranscript = uiState.liveTranscript,
                    onStop = onStopRecording,
                    onCancel = onCancelRecording
                )
            } else {
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { textDraft = it },
                    placeholder = { Text("Type a task...") },
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
                
                Text("Speak a task", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
private fun TaskVoiceRecordingStateIndicator(
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
private fun TaskActionSheet(
    task: ReminderEntity,
    onDismiss: () -> Unit,
    onEdit: (ReminderEntity) -> Unit,
    onReschedule: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceColor) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = task.task,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            Divider(color = DividerColor)
            
            TaskActionSheetRow(icon = Icons.Default.Edit, text = "Edit", onClick = { onEdit(task); onDismiss() })
            TaskActionSheetRow(icon = Icons.Default.Event, text = "Reschedule", onClick = { onReschedule(task); onDismiss() })
            TaskActionSheetRow(icon = Icons.Default.Delete, text = "Delete", tint = ErrorColor, onClick = { onDelete(task); onDismiss() })
        }
    }
}

@Composable
private fun TaskActionSheetRow(icon: ImageVector, text: String, tint: Color = TextPrimary, onClick: () -> Unit) {
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

private fun isToday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatTaskTime(millis: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return "Due ${if (isToday(millis)) "today" else "at"} · ${formatter.format(Date(millis))}"
}

private fun formatTaskDate(millis: Long): String {
    val formatter = SimpleDateFormat("EEEE · MMM d", Locale.getDefault())
    return formatter.format(Date(millis)).uppercase()
}
