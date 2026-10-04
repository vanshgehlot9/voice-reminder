package com.voicereminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voicereminder.network.WorkflowResponse
import com.voicereminder.network.WorkflowStepResponse
import com.voicereminder.ui.theme.*
import com.voicereminder.viewmodel.WorkflowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowScreen(
    onBack: () -> Unit,
    viewModel: WorkflowViewModel = viewModel()
) {
    val workflows by viewModel.workflows.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = { Text("Agent Workflows", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(workflows) { workflow ->
                WorkflowCard(workflow, onApprove = { viewModel.approveWorkflow(it) })
            }
        }
    }
}

@Composable
fun WorkflowCard(workflow: WorkflowResponse, onApprove: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = workflow.goal,
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Status: ${workflow.status}",
                color = if (workflow.status == "BLOCKED_APPROVAL") OrangeWarning else Grey500,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            workflow.steps.forEach { step ->
                WorkflowStepItem(step = step, workflowId = workflow.id, onApprove = onApprove)
            }
        }
    }
}

@Composable
fun WorkflowStepItem(step: WorkflowStepResponse, workflowId: String, onApprove: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (step.status) {
            "COMPLETED" -> Icons.Default.CheckCircle
            "PENDING" -> if (step.requiresApproval) Icons.Default.Warning else Icons.Default.RadioButtonUnchecked
            else -> Icons.Default.RadioButtonUnchecked
        }
        val tint = when (step.status) {
            "COMPLETED" -> GreenSuccess
            "PENDING" -> if (step.requiresApproval) OrangeWarning else Grey500
            else -> Violet400
        }

        Icon(
            imageVector = icon,
            contentDescription = step.status,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.description,
                color = White,
                fontSize = 14.sp
            )
            if (step.requiresApproval && step.status == "PENDING") {
                Button(
                    onClick = { onApprove(workflowId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral500),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Approve High-Risk Action", color = White)
                }
            }
        }
    }
}
