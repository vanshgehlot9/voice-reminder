package com.voicereminder.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thin wrapper around Android's MediaRecorder.
 *
 * Low-end device optimisations:
 * ─────────────────────────────
 * • Uses AAC-LC in an M4A container (or AMR-NB as fallback on very old API levels).
 *   AAC at 32kbps produces ~4KB/second vs WAV's ~176KB/second.
 *   A 5-second command becomes ~20KB instead of ~880KB — critical on 2G.
 * • Audio source = VOICE_RECOGNITION (noise suppression, voice focus by OS).
 * • 16kHz sample rate — enough for Whisper, minimal CPU/memory.
 * • Single channel (mono) — halves file size vs stereo.
 * • Low bit rate (32000 bps) — plenty for speech clarity.
 *
 * No external library required — MediaRecorder is part of the Android platform.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    /**
     * Start recording.
     *
     * @return The file where audio will be saved (exists but is being written).
     */
    fun startRecording(): File {
        stopRecording() // Safety: stop any previous session

        val file = File(context.cacheDir, "voice_command_${System.currentTimeMillis()}.m4a")
        outputFile = file

        @Suppress("DEPRECATION")
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        recorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)    // 16kHz — optimal for Whisper
            setAudioChannels(1)             // Mono — halves file size
            setAudioEncodingBitRate(32_000) // 32kbps — clear speech, tiny file

            setOutputFile(file.absolutePath)

            prepare()
            start()
        }

        return file
    }

    /**
     * Stop recording and return the completed audio file, or null if nothing was recorded.
     */
    fun stopRecording(): File? {
        val file = outputFile
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Recording was too short (< ~100ms) — delete the broken file
            file?.delete()
            return null
        } finally {
            recorder = null
            outputFile = null
        }
        return if (file?.exists() == true && (file.length() > 1024)) file else null
    }

    /**
     * Abort recording without saving.
     */
    fun cancelRecording() {
        try {
            recorder?.apply { stop(); release() }
        } catch (_: Exception) {}
        finally {
            recorder = null
            outputFile?.delete()
            outputFile = null
        }
    }

    /**
     * Delete a previously saved recording file.
     */
    fun deleteFile(file: File) {
        file.delete()
    }
}
