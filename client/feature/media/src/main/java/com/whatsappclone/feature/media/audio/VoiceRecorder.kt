package com.whatsappclone.feature.media.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class RecordingState(
    val isRecording: Boolean = false,
    val durationMs: Long = 0L,
    val amplitude: Int = 0,
    val filePath: String? = null,
    val isLocked: Boolean = false
)

@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStartTime: Long = 0L
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _amplitudes = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val amplitudes: SharedFlow<Int> = _amplitudes.asSharedFlow()

    private val cacheDir: File by lazy {
        File(context.cacheDir, "voice_notes").also { it.mkdirs() }
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(): Boolean {
        if (!hasRecordPermission()) return false
        if (_state.value.isRecording) return false

        val file = File(cacheDir, "voice_${UUID.randomUUID()}.m4a")
        recordingFile = file

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setAudioChannels(1)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()

            _state.value = RecordingState(
                isRecording = true,
                durationMs = 0L,
                amplitude = 0,
                filePath = file.absolutePath
            )

            startAmplitudeUpdates()
            return true
        } catch (e: Exception) {
            file.delete()
            recordingFile = null
            releaseRecorder()
            return false
        }
    }

    fun stopRecording(): RecordingResult? {
        if (!_state.value.isRecording) return null

        amplitudeJob?.cancel()
        amplitudeJob = null

        val duration = System.currentTimeMillis() - recordingStartTime
        val file = recordingFile

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Recording may have been too short
        }

        mediaRecorder = null
        _state.value = RecordingState()

        if (file == null || !file.exists() || duration < MIN_RECORDING_DURATION_MS) {
            file?.delete()
            return null
        }

        return RecordingResult(
            file = file,
            durationMs = duration,
            mimeType = "audio/mp4",
            sizeBytes = file.length()
        )
    }

    fun cancelRecording() {
        if (!_state.value.isRecording) return

        amplitudeJob?.cancel()
        amplitudeJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { }

        mediaRecorder = null
        recordingFile?.delete()
        recordingFile = null

        _state.value = RecordingState()
    }

    fun lockRecording() {
        if (_state.value.isRecording) {
            _state.value = _state.value.copy(isLocked = true)
        }
    }

    private fun startAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (isActive && _state.value.isRecording) {
                try {
                    val amp = mediaRecorder?.maxAmplitude ?: 0
                    val normalizedAmp = (amp.toFloat() / MAX_AMPLITUDE * 100).toInt().coerceIn(0, 100)
                    val elapsed = System.currentTimeMillis() - recordingStartTime

                    _state.value = _state.value.copy(
                        durationMs = elapsed,
                        amplitude = normalizedAmp
                    )
                    _amplitudes.tryEmit(normalizedAmp)
                } catch (_: Exception) { }

                delay(AMPLITUDE_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) { }
        mediaRecorder = null
    }

    fun release() {
        cancelRecording()
        scope.cancel()
    }

    companion object {
        private const val AMPLITUDE_UPDATE_INTERVAL_MS = 60L
        private const val MIN_RECORDING_DURATION_MS = 500L
        private const val MAX_AMPLITUDE = 32767f
    }
}

data class RecordingResult(
    val file: File,
    val durationMs: Long,
    val mimeType: String,
    val sizeBytes: Long
)
