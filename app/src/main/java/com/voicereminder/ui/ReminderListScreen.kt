package com.voicereminder.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicereminder.data.ReminderEntity
import com.voicereminder.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    reminders: List<ReminderEntity>,
    onDelete: (ReminderEntity) -> Unit,
    onBack: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Reminders", color = White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy800),
            )
        },
        containerColor = Navy900,
    ) { padding ->
        if (reminders.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Pending section
                val pending = reminders.filter { !it.isCompleted }
                val done = reminders.filter { it.isCompleted }

                if (pending.isNotEmpty()) {
                    item {
                        SectionHeader("⏳ Upcoming (${pending.size})")
                    }
                    items(pending, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            dateFormat = dateFormat,
                            onDelete = onDelete,
                        )
                    }
                }

                if (done.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)) }
                    item { SectionHeader("✅ Completed (${done.size})") }
                    items(done, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            dateFormat = dateFormat,
                            onDelete = onDelete,
                            dimmed = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Grey500,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    dateFormat: SimpleDateFormat,
    onDelete: (ReminderEntity) -> Unit,
    dimmed: Boolean = false,
) {
    val alpha = if (dimmed) 0.5f else 1f
    val now = System.currentTimeMillis()
    val isMissed = !reminder.isCompleted && reminder.scheduledAt < now

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMissed) Navy700 else Navy800,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Colored dot
            val dotColor = when {
                reminder.isCompleted -> GreenSuccess
                isMissed            -> RedError
                else                -> Violet500
            }
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = alpha))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.task,
                    color = White.copy(alpha = alpha),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                val timeText = remember(reminder.scheduledAt) {
                    dateFormat.format(Date(reminder.scheduledAt))
                }
                Text(
                    text = if (isMissed) "⚠ Missed · $timeText" else timeText,
                    color = (if (isMissed) RedError else Grey500).copy(alpha = alpha),
                    fontSize = 12.sp,
                )
                if (reminder.transcript.isNotBlank()) {
                    Text(
                        text = "\"${reminder.transcript}\"",
                        color = Grey500.copy(alpha = alpha * 0.7f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = { onDelete(reminder) }) {
                Icon(Icons.Default.Delete, "Delete", tint = Grey500)
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Notifications, null, tint = Navy700, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("No reminders yet", color = Grey500, fontSize = 16.sp)
            Text("Tap the mic and set your first reminder!", color = Grey500, fontSize = 13.sp)
        }
    }
}
