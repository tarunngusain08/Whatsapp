package com.whatsappclone.feature.media.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.roundToInt

private val RecordingRed = Color(0xFFEF5350)
private val RecordingRedDark = Color(0xFFD32F2F)
private val CancelGray = Color(0xFF8696A0)
private val SendButtonGreen = Color(0xFF00A884)
private val LockBg = Color(0xFFF0F2F5)

@Composable
fun VoiceRecordingOverlay(
    isVisible: Boolean,
    recordingState: RecordingState,
    amplitudes: SharedFlow<Int>,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    onLock: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        if (recordingState.isLocked) {
            LockedRecordingBar(
                recordingState = recordingState,
                amplitudes = amplitudes,
                onCancel = onCancel,
                onSend = onSend
            )
        } else {
            SwipeableRecordingBar(
                recordingState = recordingState,
                amplitudes = amplitudes,
                onCancel = onCancel,
                onStop = onStop,
                onLock = onLock
            )
        }
    }
}

@Composable
private fun SwipeableRecordingBar(
    recordingState: RecordingState,
    amplitudes: SharedFlow<Int>,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    onLock: () -> Unit
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val cancelThresholdPx = with(density) { 120.dp.toPx() }
    val lockThresholdPx = with(density) { 80.dp.toPx() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recording indicator + timer
                RecordingIndicator(durationMs = recordingState.durationMs)

                Spacer(modifier = Modifier.width(12.dp))

                // Waveform visualization
                LiveWaveform(
                    amplitudes = amplitudes,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Slide to cancel hint
                SlideHint(
                    offsetX = dragOffsetX,
                    modifier = Modifier.alpha(
                        (1f - ((-dragOffsetX) / cancelThresholdPx).coerceIn(0f, 1f))
                    )
                )
            }

            // Lock indicator (above mic button)
            LockIndicator(
                offsetY = dragOffsetY,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-52).dp)
                    .alpha(
                        if (dragOffsetY < -20f) {
                            ((-dragOffsetY) / lockThresholdPx).coerceIn(0f, 1f)
                        } else 0f
                    )
            )

            // Mic/Send FAB with drag gestures
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset {
                        IntOffset(
                            x = dragOffsetX.roundToInt().coerceAtMost(0),
                            y = dragOffsetY.roundToInt().coerceAtMost(0)
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (-dragOffsetX > cancelThresholdPx) {
                                    onCancel()
                                } else if (-dragOffsetY > lockThresholdPx) {
                                    onLock()
                                } else {
                                    onStop()
                                }
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                onStop()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX = (dragOffsetX + dragAmount.x).coerceAtMost(0f)
                                dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtMost(0f)
                            }
                        )
                    }
            ) {
                val pulseScale by animateFloatAsState(
                    targetValue = if (recordingState.amplitude > 30) 1.15f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "mic_pulse"
                )

                FloatingActionButton(
                    onClick = { },
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale),
                    shape = CircleShape,
                    containerColor = RecordingRed,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Recording",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedRecordingBar(
    recordingState: RecordingState,
    amplitudes: SharedFlow<Int>,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete button
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Cancel recording",
                    tint = RecordingRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Recording indicator + timer
            RecordingIndicator(durationMs = recordingState.durationMs)

            Spacer(modifier = Modifier.width(12.dp))

            // Waveform
            LiveWaveform(
                amplitudes = amplitudes,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Send button
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = SendButtonGreen,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send voice note",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordingIndicator(durationMs: Long) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Pulsing red dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(RecordingRed)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Timer
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun LiveWaveform(
    amplitudes: SharedFlow<Int>,
    modifier: Modifier = Modifier
) {
    val bars = remember { mutableStateListOf<Int>() }
    val maxBars = 48

    LaunchedEffect(amplitudes) {
        amplitudes.collect { amp ->
            bars.add(amp)
            if (bars.size > maxBars) {
                bars.removeAt(0)
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val displayBars = if (bars.isEmpty()) {
            List(maxBars) { 5 }
        } else {
            val padCount = (maxBars - bars.size).coerceAtLeast(0)
            List(padCount) { 5 } + bars.toList()
        }

        displayBars.takeLast(maxBars).forEach { amplitude ->
            val heightFraction = (amplitude.toFloat() / 100f).coerceIn(0.08f, 1f)
            val animatedHeight by animateFloatAsState(
                targetValue = heightFraction,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label = "bar_height"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp * animatedHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(RecordingRed.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
private fun SlideHint(
    offsetX: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "slide_arrow")
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = CancelGray,
            modifier = Modifier
                .size(18.dp)
                .offset(x = arrowOffset.dp)
        )

        Spacer(modifier = Modifier.width(2.dp))

        Text(
            text = "Slide to cancel",
            style = MaterialTheme.typography.bodySmall,
            color = CancelGray,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun LockIndicator(
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (offsetY < -40f) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "lock_scale"
    )

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = LockBg,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Lock recording",
                tint = CancelGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(CancelGray.copy(alpha = 0.5f))
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
