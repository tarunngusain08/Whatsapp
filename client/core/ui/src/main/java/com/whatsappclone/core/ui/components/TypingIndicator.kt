package com.whatsappclone.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TypingGreen = Color(0xFF25D366)

@Composable
fun TypingIndicator(
    names: List<String>,
    modifier: Modifier = Modifier
) {
    if (names.isEmpty()) return

    val typingText = when {
        names.size == 1 -> "${names[0]} is typing"
        names.size == 2 -> "${names[0]}, ${names[1]} are typing"
        else -> "Several people are typing"
    }

    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BouncingDots()

        Text(
            text = typingText,
            style = MaterialTheme.typography.bodySmall,
            color = TypingGreen,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun BouncingDots(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 133, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 266, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(offsetY = dot1Offset)
        Dot(offsetY = dot2Offset)
        Dot(offsetY = dot3Offset)
    }
}

@Composable
private fun Dot(
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset(y = offsetY.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(TypingGreen)
    )
}
