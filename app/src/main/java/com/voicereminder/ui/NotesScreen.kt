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
private val DividerColor = Color(0xFFE5E5EA)
private val ErrorColor = Color(0xFFFF3B30)
private val CompletedColor = Color(0xFF34C759)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<ReminderEntity>,
    uiState: HomeUiState,
    onBack: () -> Unit,
    onOpenDetail: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onSubmitText: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf("Recent") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isAddSheetOpen by rememberSaveable { mutableStateOf(false) }
    var selectedNoteForOptions by remember { mutableStateOf<ReminderEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.recordingState) {
        if (uiState.recordingState is RecordingState.Success) {
            snackbarHostState.showSnackbar("Note saved", duration = SnackbarDuration.Short)
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
            NotesTopBar(
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
                Text("New note", fontWeight = FontWeight.SemiBold)
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
                NoteFilter(selectedTab) { selectedTab = it }
            }
            
            val filteredNotes = if (isSearchActive && searchQuery.isNotBlank()) {
                notes.filter { 
                    it.task.contains(searchQuery, ignoreCase = true) || 
                    it.transcript.contains(searchQuery, ignoreCase = true) ||
                    it.replyText.contains(searchQuery, ignoreCase = true)
                }
            } else notes
            
            if (isSearchActive && searchQuery.isNotBlank()) {
                if (filteredNotes.isEmpty()) {
                    NotesEmptyState("No notes found", "Try a different search.", null)
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteRow(note, onClick = { onOpenDetail(note) }, onOpenOptions = { selectedNoteForOptions = it })
                            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            } else if (isSearchActive) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("Search notes...", color = TextSecondary, fontSize = 14.sp)
                 }
            } else {
                val sortedNotes = filteredNotes.sortedByDescending { it.createdAt }
                if (sortedNotes.isEmpty()) {
                    NotesEmptyState(
                        title = "No notes yet",
                        message = "Capture an idea by typing or speaking.",
                        icon = Icons.Default.StickyNote2
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                        item {
                            Text("${sortedNotes.size} notes", modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        items(sortedNotes, key = { it.id }) { note ->
                            NoteRow(note, onClick = { onOpenDetail(note) }, onOpenOptions = { selectedNoteForOptions = it })
                            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    if (isAddSheetOpen) {
        AddNoteSheet(
            onDismiss = { isAddSheetOpen = false },
            uiState = uiState,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onCancelRecording = onCancelRecording,
            onSubmitText = onSubmitText
        )
    }

    selectedNoteForOptions?.let { note ->
        NoteActionSheet(
            note = note,
            onDismiss = { selectedNoteForOptions = null },
            onOpen = onOpenDetail,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopBar(
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
                    placeholder = { Text("Search notes...") },
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
            title = { Text("Notes", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
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
private fun NoteFilter(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("Recent", "All")
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
private fun NoteRow(
    note: ReminderEntity,
    onClick: () -> Unit,
    onOpenOptions: (ReminderEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = note.task,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onOpenOptions(note) },
                modifier = Modifier.size(24.dp).padding(start = 8.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
        
        val isDoc = note.transcript.contains("# ") || note.transcript.length > 120
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MicNone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Note · ${formatNoteDate(note.createdAt)}",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (isDoc) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = AccentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "📄 PDF Document",
                        color = AccentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        val previewText = note.transcript.takeIf { it.isNotBlank() } ?: note.replyText
        if (previewText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = previewText,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun NotesEmptyState(title: String, message: String, icon: ImageVector?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = DividerColor, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteSheet(
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
            Text("Create note", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (uiState.recordingState != RecordingState.Idle) {
                NoteVoiceRecordingStateIndicator(
                    recordingState = uiState.recordingState,
                    liveTranscript = uiState.liveTranscript,
                    onStop = onStopRecording,
                    onCancel = onCancelRecording
                )
            } else {
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { textDraft = it },
                    placeholder = { Text("Type a note...") },
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
                
                Text("Speak a note", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
private fun NoteVoiceRecordingStateIndicator(
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
                    Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = AccentColor)) { Text("Save note") }
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
private fun NoteActionSheet(
    note: ReminderEntity,
    onDismiss: () -> Unit,
    onOpen: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceColor) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = note.task,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            Divider(color = DividerColor)
            
            NoteActionSheetRow(icon = Icons.Default.OpenInNew, text = "Open Note Details", onClick = { onOpen(note); onDismiss() })
            NoteActionSheetRow(
                icon = Icons.Default.PictureAsPdf,
                text = "View / Export PDF",
                tint = AccentColor,
                onClick = {
                    val text = note.transcript.ifBlank { note.replyText }
                    val file = com.voicereminder.util.PdfGenerator.generateDocumentPdf(context, note.task, text)
                    if (file != null) {
                        com.voicereminder.util.PdfGenerator.openPdf(context, file)
                    }
                    onDismiss()
                }
            )
            NoteActionSheetRow(
                icon = Icons.Default.Share,
                text = "Share PDF Document",
                tint = AccentColor,
                onClick = {
                    val text = note.transcript.ifBlank { note.replyText }
                    val file = com.voicereminder.util.PdfGenerator.generateDocumentPdf(context, note.task, text)
                    if (file != null) {
                        com.voicereminder.util.PdfGenerator.sharePdf(context, file)
                    }
                    onDismiss()
                }
            )
            NoteActionSheetRow(icon = Icons.Default.Delete, text = "Delete", tint = ErrorColor, onClick = { onDelete(note); onDismiss() })
        }
    }
}

@Composable
private fun NoteActionSheetRow(icon: ImageVector, text: String, tint: Color = TextPrimary, onClick: () -> Unit) {
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

private fun formatNoteDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
