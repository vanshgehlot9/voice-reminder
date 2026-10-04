package com.voicereminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voicereminder.network.MemoryResponse
import com.voicereminder.network.ProjectResponse
import com.voicereminder.ui.theme.*
import com.voicereminder.viewmodel.HomeUiState
import com.voicereminder.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    uiState: HomeUiState,
    onBack: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissState: () -> Unit,
    onAttachFile: () -> Unit,
    onCaptureDocument: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitText: (String) -> Unit,
    viewModel: WorkspaceViewModel = viewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val memories by viewModel.memories.collectAsState()
    var commandDraft by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = { Text("Workspace", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        },
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
                    onSubmitText(it)
                    commandDraft = ""
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Active Projects",
                color = Coral500,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(project = project)
                }
            }

            Text(
                "Memory Graph",
                color = Coral500,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(memories) { memory ->
                    MemoryItem(memory = memory)
                }
            }
        }
    }
}

@Composable
fun ProjectCard(project: ProjectResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { /* TODO: Navigate to detail */ },
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = project.title,
                color = White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 2
            )
            Text(
                text = "${project.reports.size} Reports",
                color = Grey500,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MemoryItem(memory: MemoryResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = memory.category.uppercase(),
                color = Violet400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = memory.content,
                color = White50,
                fontSize = 14.sp
            )
        }
    }
}
