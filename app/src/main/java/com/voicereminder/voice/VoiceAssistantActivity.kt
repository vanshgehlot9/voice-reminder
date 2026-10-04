package com.voicereminder.voice

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Translucent floating Assistant Activity that appears on top of the
 * Android Home Screen or ANY other app (WhatsApp, YouTube, Browser, etc.)
 * when the user says "Hey" or "Hello".
 *
 * It uses Theme.VoiceReminder.Translucent so the background app/home screen
 * remains visible underneath with a sleek dimming effect, just like Apple Siri.
 */
class VoiceAssistantActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure activity can show over lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            val sessionManager = VoiceSessionManager.instance
            if (sessionManager == null) {
                finish()
                return@setContent
            }

            val uiState by sessionManager.uiState.collectAsState()

            // When voice interaction is completed and back to IDLE, finish activity automatically
            LaunchedEffect(uiState.state) {
                if (uiState.state == VoiceState.IDLE) {
                    finish()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        sessionManager.cancel()
                        finish()
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                VoiceActivationOverlay(
                    uiState = uiState,
                    onCancel = {
                        sessionManager.cancel()
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
