package com.voicereminder.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun VoiceActivationOverlay(
    uiState: VoiceUiState,
    onCancel: () -> Unit
) {
    if (uiState.state == VoiceState.IDLE || uiState.state == VoiceState.LISTENING_FOR_WAKE_WORD) {
        return
    }

    // Black semi-transparent background overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Voice interaction surface
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false) {}, // Prevent clicks from passing through
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF1C1C1E),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (uiState.state) {
                        VoiceState.WAKE_WORD_DETECTED, VoiceState.ACTIVATING -> {
                            VoiceActivationAnimation()
                        }
                        VoiceState.LISTENING_FOR_COMMAND -> {
                            VoiceListeningView(uiState.transcript, uiState.waveformAmplitude)
                        }
                        VoiceState.PROCESSING_COMMAND -> {
                            VoiceProcessingView(uiState.transcript)
                        }
                        VoiceState.SPEAKING_RESPONSE -> {
                            VoiceSuccessView(uiState.response)
                        }
                        VoiceState.ERROR -> {
                            VoiceErrorView(uiState.error)
                        }
                        else -> {}
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Cancel button
                    if (uiState.state == VoiceState.LISTENING_FOR_COMMAND || uiState.state == VoiceState.WAKE_WORD_DETECTED) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF3A3A3C),
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(onClick = onCancel)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceActivationAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Glowing outer halo
            Box(
                modifier = Modifier
                    .size(90.dp * scale)
                    .clip(CircleShape)
                    .background(Color(0xFF5E5CE6).copy(alpha = 0.25f))
            )
            // Core glowing orb
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFF312E81))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("I'm listening...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Speak your reminder or command", color = Color(0xFFA1A1AA), fontSize = 14.sp)
    }
}

@Composable
private fun VoiceListeningView(transcript: String, amplitude: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Siri-like Multi-Frequency Wave
        OriginalVoiceWave(amplitude)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF27272A),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (transcript.isBlank()) "Listening..." else transcript,
                    color = if (transcript.isBlank()) Color(0xFFA1A1AA) else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }
        }
    }
}

@Composable
private fun OriginalVoiceWave(amplitude: Float) {
    val smoothAmplitude by animateFloatAsState(
        targetValue = amplitude.coerceIn(0.15f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "amplitude"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "wave_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "phase"
    )

    // Siri gradient colors
    val barColors = listOf(
        Color(0xFF38BDF8), // Sky blue
        Color(0xFF818CF8), // Indigo
        Color(0xFFC084FC), // Violet
        Color(0xFFF472B6), // Pink
        Color(0xFFC084FC), // Violet
        Color(0xFF818CF8), // Indigo
        Color(0xFF34D399), // Emerald
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(72.dp)
    ) {
        val barCount = barColors.size
        for (i in 0 until barCount) {
            val barPhase = phase + (i * 0.9f)
            val heightMultiplier = (Math.sin(barPhase.toDouble()).toFloat() * 0.5f + 0.5f) * smoothAmplitude
            val height = (14.dp + (52.dp * heightMultiplier)).coerceIn(12.dp, 68.dp)
            
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(barColors[i])
            )
        }
    }
}

@Composable
private fun VoiceProcessingView(transcript: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(
            color = Color(0xFF818CF8),
            strokeWidth = 3.dp,
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text("Processing...", color = Color(0xFF818CF8), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\"$transcript\"",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun VoiceSuccessView(response: VoiceCommandResult?) {
    val intent = response?.intent ?: ""
    val (icon, bgColor) = when (intent) {
        "call" -> Pair(Icons.Default.Done, Color(0xFF10B981))
        "play_music" -> Pair(Icons.Default.Done, Color(0xFFF43F5E))
        "open_app", "web_search" -> Pair(Icons.Default.Done, Color(0xFF3B82F6))
        "create_document", "create_note" -> Pair(Icons.Default.Done, Color(0xFF8B5CF6))
        else -> Pair(Icons.Default.Done, Color(0xFF10B981))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = response?.message ?: "Done!",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        if (intent in listOf("create_document", "create_note")) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF27272A),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "📄 Enhanced Document & PDF Saved to Notes",
                    color = Color(0xFFA78BFA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceErrorView(error: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sorry, I didn't catch that.",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = error, color = Color(0xFFA1A1AA), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun IdleVoiceIndicator(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Subtle microphone indicator when idle
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE5E5EA).copy(alpha = 0.5f),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5E5CE6))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Voice ready", color = Color(0xFF1D1D1F), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
