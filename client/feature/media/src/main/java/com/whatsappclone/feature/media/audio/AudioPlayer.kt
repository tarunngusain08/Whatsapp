package com.whatsappclone.feature.media.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val progress: Float = 0f
)

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var positionJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1f, 1f)
            }
        }
    }

    fun play(messageId: String, audioSource: String) {
        val currentState = _playbackState.value

        // If same message and just paused, resume
        if (currentState.activeMessageId == messageId && !currentState.isPlaying && mediaPlayer != null) {
            resume()
            return
        }

        // If different message, stop current and start new
        if (currentState.activeMessageId != messageId) {
            stopInternal()
        }

        if (!requestAudioFocus()) return

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioSource)
                prepare()

                setOnCompletionListener {
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            currentPositionMs = 0L,
                            progress = 0f
                        )
                    }
                    stopPositionUpdates()
                    abandonAudioFocus()
                }

                setOnErrorListener { _, _, _ ->
                    stopInternal()
                    true
                }
            }

            mediaPlayer = player

            // Apply current speed setting
            val speed = _playbackState.value.playbackSpeed
            player.playbackParams = player.playbackParams.setSpeed(speed)
            player.start()

            _playbackState.value = PlaybackState(
                activeMessageId = messageId,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = player.duration.toLong(),
                playbackSpeed = speed,
                progress = 0f
            )

            startPositionUpdates()
        } catch (e: Exception) {
            stopInternal()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _playbackState.update { it.copy(isPlaying = false) }
            stopPositionUpdates()
        } catch (_: Exception) { }
    }

    fun resume() {
        if (!requestAudioFocus()) return
        try {
            mediaPlayer?.start()
            _playbackState.update { it.copy(isPlaying = true) }
            startPositionUpdates()
        } catch (_: Exception) { }
    }

    fun stop() {
        stopInternal()
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            val duration = _playbackState.value.durationMs
            val progress = if (duration > 0) positionMs.toFloat() / duration else 0f
            _playbackState.update {
                it.copy(
                    currentPositionMs = positionMs,
                    progress = progress.coerceIn(0f, 1f)
                )
            }
        } catch (_: Exception) { }
    }

    fun seekToFraction(fraction: Float) {
        val duration = _playbackState.value.durationMs
        if (duration > 0) {
            seekTo((fraction * duration).toLong())
        }
    }

    fun toggleSpeed() {
        val nextSpeed = when (_playbackState.value.playbackSpeed) {
            1f -> 1.5f
            1.5f -> 2f
            else -> 1f
        }
        setPlaybackSpeed(nextSpeed)
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            mediaPlayer?.let { player ->
                player.playbackParams = player.playbackParams.setSpeed(speed)
            }
            _playbackState.update { it.copy(playbackSpeed = speed) }
        } catch (_: Exception) { }
    }

    private fun stopInternal() {
        stopPositionUpdates()
        abandonAudioFocus()

        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { }

        mediaPlayer = null
        _playbackState.value = PlaybackState(
            playbackSpeed = _playbackState.value.playbackSpeed
        )
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionJob = scope.launch {
            while (isActive) {
                try {
                    val player = mediaPlayer ?: break
                    val position = player.currentPosition.toLong()
                    val duration = player.duration.toLong()
                    val progress = if (duration > 0) position.toFloat() / duration else 0f

                    _playbackState.update {
                        it.copy(
                            currentPositionMs = position,
                            durationMs = duration,
                            progress = progress.coerceIn(0f, 1f)
                        )
                    }
                } catch (_: Exception) {
                    break
                }
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun requestAudioFocus(): Boolean {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

        audioFocusRequest = focusRequest
        return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    fun release() {
        stopInternal()
        scope.cancel()
    }

    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 100L
    }
}
