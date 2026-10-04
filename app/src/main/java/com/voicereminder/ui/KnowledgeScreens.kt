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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class NoteItem(
    val title: String,
    val body: String,
    val kind: String,
    val isPinned: Boolean = false,
    val duration: String? = null,
    val hasWaveform: Boolean = false,
)

private data class DocumentItem(
    val filename: String,
    val type: String,
    val size: String,
    val modified: String,
)

private data class ResearchSource(
    val title: String,
    val citation: String,
)

private data class MemoryCategory(
    val title: String,
    val description: String,
    val items: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OldNotesScreen(
    onBack: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitText: (String) -> Unit,
) {
    val notes = remember {
        listOf(
            NoteItem("Project ideas", "Voice note captured from a commute brainstorm.", "Voice note", isPinned = true, duration = "01:42", hasWaveform = true),
            NoteItem("Client call summary", "Draft of action items and follow-ups.", "Text note", isPinned = true),
            NoteItem("Reading list", "Books and articles to review this weekend.", "Text note"),
            NoteItem("Launch retrospective", "What worked, what slowed us down, next steps.", "Recent note"),
        )
    }
    var selectedTab by rememberSaveable { mutableStateOf("Recent") }
    var commandDraft by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = { Text("Notes", color = White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
            )
        },
        bottomBar = {
            UniversalCommandBar(
                recordingState = RecordingState.Idle,
                liveTranscript = "",
                voiceRmsDb = 0f,
                recordingStartedAtMs = null,
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
                    onSubmitText(it)
                    commandDraft = ""
                },
            )
        },
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Editorial notes", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Text notes, voice notes, pinned ideas, and recent captures in one calm workspace.", color = Grey500, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Recent", "Pinned", "Voice", "Text").forEach { tab ->
                                FilterChip(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    label = { Text(tab) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Violet400,
                                        selectedLabelColor = Navy900,
                                        containerColor = Navy700,
                                        labelColor = White,
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f)),
                                )
                            }
                        }
                    }
                }
            }

            items(notes.filter {
                when (selectedTab) {
                    "Pinned" -> it.isPinned
                    "Voice" -> it.kind.contains("voice", ignoreCase = true)
                    "Text" -> it.kind.contains("text", ignoreCase = true)
                    else -> true
                }
            }) { note ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Navy800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (note.isPinned) Coral500 else Violet400))
                            Text(note.kind, color = Grey500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            if (note.isPinned) {
                                Text("Pinned", color = Coral500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Grey500)
                        }
                        Text(note.title, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(note.body, color = Grey300, fontSize = 12.sp)
                        if (note.duration != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(note.duration, color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Waveform preview", color = Grey500, fontSize = 11.sp)
                            }
                            VoiceWaveformPreview()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveformPreview() {
    val bars = listOf(0.25f, 0.42f, 0.68f, 0.54f, 0.77f, 0.46f, 0.30f, 0.58f)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        bars.forEachIndexed { index, bar ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((bar * 28f).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index % 2 == 0) Violet400.copy(alpha = 0.35f) else Coral500.copy(alpha = 0.3f))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    onOpenDocument: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitText: (String) -> Unit,
) {
    val documents = remember {
        listOf(
            DocumentItem("Quarterly Plan.pdf", "PDF", "2.4 MB", "Today"),
            DocumentItem("Client Brief.docx", "DOCX", "860 KB", "Yesterday"),
            DocumentItem("Brand Reference.png", "Image", "1.1 MB", "Mon"),
            DocumentItem("Expense Policy.pdf", "PDF", "740 KB", "Sep 20"),
        )
    }
    var search by rememberSaveable { mutableStateOf("") }
    var commandDraft by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = { Text("Documents", color = White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White) }
                },
                actions = {
                    IconButton(onClick = onAttachFile) { Icon(Icons.Default.UploadFile, contentDescription = null, tint = White) }
                    IconButton(onClick = onCaptureDocument) { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
            )
        },
        bottomBar = {
            UniversalCommandBar(
                recordingState = RecordingState.Idle,
                liveTranscript = "",
                voiceRmsDb = 0f,
                recordingStartedAtMs = null,
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
                    onSubmitText(it)
                    commandDraft = ""
                },
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
                Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Professional document workspace", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Searchable files with AI actions for summarizing, explaining, extracting, and translating.", color = Grey500, fontSize = 12.sp)
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search documents", color = Grey500) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Grey500) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedContainerColor = Navy700,
                                unfocusedContainerColor = Navy700,
                                focusedBorderColor = Violet400,
                                unfocusedBorderColor = Navy600,
                            ),
                        )
                    }
                }
            }

            items(documents.filter { it.filename.contains(search, ignoreCase = true) }) { doc ->
                Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Violet400.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Violet400)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.filename, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("${doc.type} · ${doc.size} · ${doc.modified}", color = Grey500, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Grey500)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionChip("Summarize")
                            ActionChip("Explain")
                            ActionChip("Search")
                            ActionChip("Extract")
                            ActionChip("Translate")
                        }
                        Surface(onClick = { onOpenDocument(doc.filename) }, shape = RoundedCornerShape(999.dp), color = Violet400) {
                            Text("Open document intelligence", color = Navy900, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = Navy700, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
        Text(text, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentAIViewScreen(
    documentName: String,
    onBack: () -> Unit,
) {
    var question by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = { Text(documentName, color = White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Document visible", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("The assistant answers while keeping the document in context.", color = Grey500, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)).background(Navy700), contentAlignment = Alignment.Center) {
                        Text("[ Document preview stays visible here ]", color = Grey500)
                    }
                }
            }

            Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionChip("Summarize document")
                        ActionChip("Explain this section")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionChip("Find a topic")
                        ActionChip("Extract key points")
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ask about this document...", color = Grey500) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedContainerColor = Navy700,
                            unfocusedContainerColor = Navy700,
                            focusedBorderColor = Violet400,
                            unfocusedBorderColor = Navy600,
                        ),
                    )
                    Text("Contextual answer appears here while the document remains visible.", color = Grey300, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchScreen(
    onBack: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitText: (String) -> Unit,
) {
    var commandDraft by rememberSaveable { mutableStateOf("") }
    val sources = remember {
        listOf(
            ResearchSource("Android developers blog", "Source 1 · 2026"),
            ResearchSource("AOSP docs", "Source 2 · 2026"),
            ResearchSource("Platform release notes", "Source 3 · 2026"),
        )
    }
    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = { Text("Research", color = White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
            )
        },
        bottomBar = {
            UniversalCommandBar(
                recordingState = RecordingState.Idle,
                liveTranscript = "",
                voiceRmsDb = 0f,
                recordingStartedAtMs = null,
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
                    onSubmitText(it)
                    commandDraft = ""
                },
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
                Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Research workspace", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Natural-language research requests become structured reports.", color = Grey500, fontSize = 12.sp)
                        Text("Research query", color = Grey500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Research the latest Android background execution restrictions.", color = White, fontSize = 15.sp)
                    }
                }
            }

            item {
                ResearchSectionCard("Key findings", listOf("Background tasks are increasingly constrained.", "Foreground services need user-visible intent.", "Exact scheduling requires permission-sensitive handling."))
                ResearchSectionCard("Important facts", listOf("Doze mode affects timing.", "Battery optimizations may defer work.", "User-facing disclosure is important."))
                ResearchSectionCard("Source references", sources.map { "${it.title} · ${it.citation}" })
                ResearchSectionCard("Summary", listOf("Use WorkManager for deferrable work, exact alarms for user-initiated reminders, and foreground service only when the UI warrants it."))
            }
        }
    }
}

@Composable
private fun ResearchSectionCard(title: String, bullets: List<String>) {
    Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            bullets.forEach { bullet ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Coral500))
                    Text(bullet, color = Grey300, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitText: (String) -> Unit,
) {
    val categories = remember {
        listOf(
            MemoryCategory("Preferences", "Things the assistant should remember about how you like to work.", listOf("Prefer calm, minimal UI", "Use 5 PM reminders for evening planning")),
            MemoryCategory("Projects", "Active workstreams and their current context.", listOf("Voice reminder app", "Personal knowledge workspace")),
            MemoryCategory("Important context", "Facts that matter across sessions.", listOf("Uses Android Studio on macOS", "Prefers native Android components")),
            MemoryCategory("Recurring patterns", "Repeated behaviors or habits detected over time.", listOf("Sets reminders after meetings", "Turns notes into tasks")),
        )
    }
    var expandedCategory by rememberSaveable { mutableStateOf("Preferences") }
    var commandDraft by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = { Text("Memory", color = White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
            )
        },
        bottomBar = {
            UniversalCommandBar(
                recordingState = RecordingState.Idle,
                liveTranscript = "",
                voiceRmsDb = 0f,
                recordingStartedAtMs = null,
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
                    onSubmitText(it)
                    commandDraft = ""
                },
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
                Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Personal memory", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Intentional retention presented as understandable categories.", color = Grey500, fontSize = 12.sp)
                    }
                }
            }

            items(categories) { category ->
                Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(category.title, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(category.description, color = Grey500, fontSize = 12.sp)
                            }
                            Surface(shape = RoundedCornerShape(999.dp), color = Navy700) {
                                Text(if (expandedCategory == category.title) "Expanded" else "View", color = White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                        category.items.forEach { memory ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Violet400))
                                Text(memory, color = Grey300, fontSize = 12.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(999.dp), color = Violet400) { Text("Update", color = Navy900, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                            Surface(shape = RoundedCornerShape(999.dp), color = Navy700, border = androidx.compose.foundation.BorderStroke(1.dp, RedError.copy(alpha = 0.3f))) { Text("Remove", color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OldSettingsScreen(
    onBack: () -> Unit,
) {
    var voiceWakeWord by rememberSaveable { mutableStateOf(true) }
    var aiBehavior by rememberSaveable { mutableStateOf(true) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var memory by rememberSaveable { mutableStateOf(true) }
    var language by rememberSaveable { mutableStateOf(true) }
    var appearance by rememberSaveable { mutableStateOf(true) }
    var privacy by rememberSaveable { mutableStateOf(true) }
    var storage by rememberSaveable { mutableStateOf(true) }

    val rows = listOf(
        "Voice & Wake Word" to voiceWakeWord,
        "AI Behavior" to aiBehavior,
        "Notifications" to notifications,
        "Memory" to memory,
        "Language" to language,
        "Appearance" to appearance,
        "Privacy & Permissions" to privacy,
        "Data & Storage" to storage,
    )

    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Minimal Android settings", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Native patterns, clear sections, no decorative clutter.", color = Grey500, fontSize = 12.sp)
                    }
                }
            }

            items(rows) { (label, enabled) ->
                Surface(shape = RoundedCornerShape(20.dp), color = Navy800, border = androidx.compose.foundation.BorderStroke(1.dp, Navy600.copy(alpha = 0.45f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(label, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Configure $label", color = Grey500, fontSize = 12.sp)
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { checked ->
                                when (label) {
                                    "Voice & Wake Word" -> voiceWakeWord = checked
                                    "AI Behavior" -> aiBehavior = checked
                                    "Notifications" -> notifications = checked
                                    "Memory" -> memory = checked
                                    "Language" -> language = checked
                                    "Appearance" -> appearance = checked
                                    "Privacy & Permissions" -> privacy = checked
                                    "Data & Storage" -> storage = checked
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Navy900,
                                checkedTrackColor = Violet400,
                                uncheckedThumbColor = Grey300,
                                uncheckedTrackColor = Navy600,
                            ),
                        )
                    }
                }
            }
        }
    }
}
