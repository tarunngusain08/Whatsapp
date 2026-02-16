package com.whatsappclone.feature.media.audio

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsappclone.core.ui.components.MessageStatusIcon
import com.whatsappclone.core.ui.theme.WhatsAppColors

private val PlayButtonGreen = Color(0xFF00A884)
private val WaveformPlayed = Color(0xFF25D366)
private val WaveformUnplayed = Color(0xFFB0BEC5)
private val WaveformPlayedOwn = Color(0xFF53BDEB)
private val WaveformUnplayedOwn = Color(0xFFA8DADC)
private val MetaTextColor = Color(0xFF667781)
private val SpeedButtonBg = Color(0x1A000000)

private val GroupSenderColors = listOf(
    Color(0xFF1FA855),
    Color(0xFF6B4CE0),
    Color(0xFFE06B4C),
    Color(0xFF4CA8E0),
    Color(0xFFE04C9B),
    Color(0xFFE09B4C),
)

@Composable
fun VoiceNoteBubble(
    messageId: String,
    senderId: String,
    senderName: String?,
    isOwnMessage: Boolean,
    isGroupChat: Boolean,
    durationMs: Long,
    formattedTime: String,
    messageStatus: String,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.78f

    val bubbleColor = if (isOwnMessage) {
        WhatsAppColors.SentBubble
    } else {
        WhatsAppColors.ReceivedBubble
    }

    val bubbleShape = remember(isOwnMessage) {
        if (isOwnMessage) {
            RoundedCornerShape(
                topStart = 12.dp, topEnd = 4.dp,
                bottomStart = 12.dp, bottomEnd = 12.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 4.dp, topEnd = 12.dp,
                bottomStart = 12.dp, bottomEnd = 12.dp
            )
        }
    }

    val alignment = if (isOwnMessage) Alignment.CenterEnd else Alignment.CenterStart

    val isThisMessageActive = playbackState.activeMessageId == messageId
    val isPlaying = isThisMessageActive && playbackState.isPlaying
    val progress = if (isThisMessageActive) playbackState.progress else 0f
    val currentPosition = if (isThisMessageActive) playbackState.currentPositionMs else 0L
    val speed = if (isThisMessageActive) playbackState.playbackSpeed else 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isOwnMessage) 48.dp else 6.dp,
                end = if (isOwnMessage) 6.dp else 48.dp,
                top = 1.dp,
                bottom = 1.dp
            ),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 0.5.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.widthIn(min = 200.dp, max = maxBubbleWidth)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Sender name for group chats
                if (isGroupChat && !isOwnMessage && senderName != null) {
                    val senderColor = remember(senderId) {
                        GroupSenderColors[senderId.hashCode().mod(GroupSenderColors.size).let {
                            if (it < 0) it + GroupSenderColors.size else it
                        }]
                    }
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = senderColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Play/Pause button
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onPlayPause() },
                        shape = CircleShape,
                        color = PlayButtonGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Waveform + duration
                    Column(modifier = Modifier.weight(1f)) {
                        StaticWaveform(
                            progress = progress,
                            isOwnMessage = isOwnMessage,
                            messageId = messageId,
                            onSeek = onSeek,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Duration / position + speed button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isThisMessageActive && currentPosition > 0) {
                                    formatDurationShort(currentPosition)
                                } else {
                                    formatDurationShort(durationMs)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MetaTextColor,
                                fontSize = 11.sp
                            )

                            // Playback speed button
                            if (isThisMessageActive) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onToggleSpeed() },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SpeedButtonBg
                                ) {
                                    Text(
                                        text = formatSpeed(speed),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Timestamp + status row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MetaTextColor,
                        fontSize = 11.sp
                    )

                    if (isOwnMessage) {
                        Spacer(modifier = Modifier.width(3.dp))
                        MessageStatusIcon(
                            status = messageStatus,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaticWaveform(
    progress: Float,
    isOwnMessage: Boolean,
    messageId: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val barCount = 40
    val barHeights = remember(messageId) {
        List(barCount) { ((it * 7 + messageId.hashCode()) % 100).let { v ->
            (v.toFloat().mod(80f) + 20f) / 100f
        }}
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "waveform_progress"
    )

    val playedColor = if (isOwnMessage) WaveformPlayedOwn else WaveformPlayed
    val unplayedColor = if (isOwnMessage) WaveformUnplayedOwn else WaveformUnplayed

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        barHeights.forEachIndexed { index, heightFraction ->
            val barProgress = index.toFloat() / barCount
            val color = if (barProgress <= animatedProgress) playedColor else unplayedColor

            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(28.dp * heightFraction.coerceIn(0.12f, 1f))
                    .clip(RoundedCornerShape(1.25.dp))
                    .background(color)
            )
        }
    }
}

private fun formatDurationShort(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSpeed(speed: Float): String {
    return when (speed) {
        1f -> "1x"
        1.5f -> "1.5x"
        2f -> "2x"
        else -> "${speed}x"
    }
}
