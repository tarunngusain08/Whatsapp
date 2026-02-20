package com.whatsappclone.feature.chat.ui.chatdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.whatsappclone.core.common.util.UrlResolver

private val QuotedReplyColors = listOf(
    Color(0xFF1A73E8),
    Color(0xFF9C27B0),
    Color(0xFFE65100),
    Color(0xFF00838F),
    Color(0xFF2E7D32),
    Color(0xFFC62828),
    Color(0xFF4527A0),
    Color(0xFF00695C),
    Color(0xFFAD1457),
    Color(0xFF283593),
    Color(0xFF558B2F),
    Color(0xFF6A1B9A)
)

private fun quotedSenderColor(name: String?): Color {
    val hash = (name?.hashCode() ?: 0) and 0x7FFFFFFF
    return QuotedReplyColors[hash % QuotedReplyColors.size]
}

@Composable
fun QuotedReplyCard(
    senderName: String?,
    content: String?,
    type: String?,
    thumbnailUrl: String?,
    isOwnMessageQuote: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(senderName, isOwnMessageQuote) {
        if (isOwnMessageQuote) Color(0xFF00A884)
        else quotedSenderColor(senderName)
    }

    val bgColor = if (isOwnMessageQuote) {
        Color(0x1A00A884)
    } else {
        Color(0x1A000000)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (thumbnailUrl != null) 56.dp else 48.dp)
                .background(accentColor)
        )

        // Text content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = senderName ?: "Unknown",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            val displayText = content ?: when (type) {
                "image" -> "\uD83D\uDCF7 Photo"
                "video" -> "\uD83C\uDFA5 Video"
                "audio" -> "\uD83C\uDFA4 Audio"
                "document" -> "\uD83D\uDCC4 Document"
                else -> ""
            }

            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Thumbnail for media replies
        if (thumbnailUrl != null) {
            AsyncImage(
                model = UrlResolver.resolve(thumbnailUrl),
                contentDescription = "Replied media",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
            )
        }
    }
}
