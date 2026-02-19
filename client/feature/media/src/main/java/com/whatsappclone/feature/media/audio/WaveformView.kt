package com.whatsappclone.feature.media.audio

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun WaveformView(
    amplitudes: List<Float>,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    modifier: Modifier = Modifier,
    barWidthDp: Float = 2.5f,
    barGapDp: Float = 1.5f,
    minBarFraction: Float = 0.1f,
    onSeek: ((Float) -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "waveform_progress"
    )

    Canvas(
        modifier = modifier
            .then(
                if (onSeek != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val seekFraction = if (size.width > 0) (offset.x / size.width).coerceIn(0f, 1f) else 0f
                            onSeek(seekFraction)
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val barWidth = barWidthDp.dp.toPx()
        val barGap = barGapDp.dp.toPx()
        val totalBarSlot = barWidth + barGap
        val barCount = ((size.width + barGap) / totalBarSlot).toInt().coerceAtLeast(1)
        val canvasHeight = size.height
        val cornerPx = barWidth / 2f

        for (i in 0 until barCount) {
            val ampIndex = if (amplitudes.isNotEmpty()) {
                (i * amplitudes.size / barCount).coerceIn(0, amplitudes.lastIndex)
            } else 0

            val amplitude = if (amplitudes.isNotEmpty()) {
                amplitudes[ampIndex].coerceIn(minBarFraction, 1f)
            } else {
                minBarFraction
            }

            val barHeight = canvasHeight * amplitude
            val x = i * totalBarSlot
            val y = (canvasHeight - barHeight) / 2f

            val barFraction = x / size.width
            val color = if (barFraction <= animatedProgress) playedColor else unplayedColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )
        }
    }
}

@Composable
fun LiveWaveformView(
    amplitudes: List<Float>,
    barColor: Color,
    modifier: Modifier = Modifier,
    barWidthDp: Float = 3f,
    barGapDp: Float = 2f,
    minBarFraction: Float = 0.08f
) {
    Canvas(modifier = modifier) {
        val barWidth = barWidthDp.dp.toPx()
        val barGap = barGapDp.dp.toPx()
        val totalBarSlot = barWidth + barGap
        val maxBars = ((size.width + barGap) / totalBarSlot).toInt().coerceAtLeast(1)
        val canvasHeight = size.height
        val cornerPx = barWidth / 2f

        val displayBars = if (amplitudes.size >= maxBars) {
            amplitudes.takeLast(maxBars)
        } else {
            val padCount = maxBars - amplitudes.size
            List(padCount) { minBarFraction } + amplitudes
        }

        displayBars.forEachIndexed { i, amp ->
            val amplitude = amp.coerceIn(minBarFraction, 1f)
            val barHeight = canvasHeight * amplitude
            val x = i * totalBarSlot
            val y = (canvasHeight - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )
        }
    }
}
