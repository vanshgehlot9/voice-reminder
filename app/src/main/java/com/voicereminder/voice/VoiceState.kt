package com.voicereminder.voice

enum class VoiceState {
    IDLE,
    STARTING_WAKE_WORD,
    LISTENING_FOR_WAKE_WORD,
    WAKE_WORD_DETECTED,
    ACTIVATING,
    LISTENING_FOR_COMMAND,
    PROCESSING_COMMAND,
    SPEAKING_RESPONSE,
    ERROR
}

data class VoiceSession(
    val sessionId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val wakeWordDetectedAt: Long? = null,
    val commandStartedAt: Long? = null,
    val commandText: String? = null,
    val result: VoiceCommandResult? = null,
    val endedAt: Long? = null
)

data class VoiceCommandResult(
    val success: Boolean,
    val intent: String,
    val message: String,
    val entityId: String? = null,
    val shouldSpeak: Boolean = true
)

data class VoiceUiState(
    val state: VoiceState = VoiceState.IDLE,
    val wakePhrase: String = "Hey VoiceReminder",
    val transcript: String = "",
    val waveformAmplitude: Float = 0f,
    val isWakeWordEnabled: Boolean = true,
    val error: String? = null,
    val response: VoiceCommandResult? = null
)

data class WakeWordConfig(
    val enabled: Boolean = true,
    val wakePhrase: String = "Hey VoiceReminder",
    val sensitivity: Float = 0.5f,
    val cooldownMs: Long = 3000L
)
