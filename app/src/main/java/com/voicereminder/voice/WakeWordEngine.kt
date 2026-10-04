package com.voicereminder.voice

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback

interface WakeWordEngine {
    fun start(onWakeWordDetected: (transcript: String?) -> Unit, onError: (String) -> Unit)
    fun stop()
    fun destroy()
}

class PorcupineWakeWordEngine(private val context: Context, private val config: WakeWordConfig) : WakeWordEngine {
    private val TAG = "PorcupineWakeWordEngine"
    private var porcupineManager: PorcupineManager? = null
    
    // Replace this with a valid Picovoice AccessKey to enable real wake word detection.
    private val ACCESS_KEY = "YOUR_PICOVOICE_API_KEY"

    override fun start(onWakeWordDetected: (transcript: String?) -> Unit, onError: (String) -> Unit) {
        try {
            if (porcupineManager != null) {
                Log.w(TAG, "Wake word engine already running")
                return
            }
            
            if (ACCESS_KEY == "YOUR_PICOVOICE_API_KEY") {
                Log.w(TAG, "Please provide a valid Picovoice AccessKey in PorcupineWakeWordEngine to use the real engine.")
                onError("Missing wake word model/key.")
                return
            }

            // Using standard built-in keyword for demonstration (e.g. "HEY_SIRI" or "PORCUPINE")
            // A custom "Hey VoiceReminder" model would be loaded via file path here.
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeyword(Porcupine.BuiltInKeyword.PORCUPINE) 
                .setSensitivity(config.sensitivity)
                .build(context, PorcupineManagerCallback { keywordIndex ->
                    if (keywordIndex == 0) {
                        Log.d(TAG, "Wake word detected!")
                        onWakeWordDetected(null)
                    }
                })

            porcupineManager?.start()
            Log.d(TAG, "Wake word engine started successfully.")

        } catch (e: PorcupineException) {
            Log.e(TAG, "Failed to initialize Porcupine", e)
            onError(e.message ?: "Failed to initialize wake word engine")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in wake word engine", e)
            onError(e.message ?: "Unexpected error")
        }
    }

    override fun stop() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
            Log.d(TAG, "Wake word engine stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping wake word engine", e)
        }
    }

    override fun destroy() {
        stop()
    }
}
