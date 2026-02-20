package com.whatsappclone.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.whatsappclone.core.common.util.UrlResolver

@Composable
fun UserAvatar(
    url: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null
) {
    val avatarModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )

    val resolvedUrl = UrlResolver.resolve(url)

    if (!resolvedUrl.isNullOrBlank()) {
        Surface(
            modifier = avatarModifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            AsyncImage(
                model = resolvedUrl,
                contentDescription = "$name avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    } else {
        val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val backgroundColor = generateAvatarColor(name)
        val fontSize = (size.value * 0.42f).sp

        Box(
            modifier = avatarModifier
                .background(color = backgroundColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun generateAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF1ABC9C),
        Color(0xFF2ECC71),
        Color(0xFF3498DB),
        Color(0xFF9B59B6),
        Color(0xFFE67E22),
        Color(0xFFE74C3C),
        Color(0xFF1A5276),
        Color(0xFF27AE60),
        Color(0xFF2980B9),
        Color(0xFF8E44AD),
        Color(0xFFD35400),
        Color(0xFFC0392B)
    )
    val index = (name.hashCode() and 0x7FFFFFFF) % colors.size
    return colors[index]
}
