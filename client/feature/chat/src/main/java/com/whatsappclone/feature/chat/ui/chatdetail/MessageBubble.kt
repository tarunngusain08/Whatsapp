package com.whatsappclone.feature.chat.ui.chatdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsappclone.core.ui.components.MessageStatusIcon
import com.whatsappclone.core.ui.theme.WhatsAppColors
import com.whatsappclone.feature.chat.model.MessageUi

private val SenderNameColors = listOf(
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageUi,
    isGroupChat: Boolean,
    onLongPress: (MessageUi) -> Unit,
    onQuotedReplyClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.85f

    val bubbleColor = if (message.isOwnMessage) {
        WhatsAppColors.SentBubble
    } else {
        WhatsAppColors.ReceivedBubble
    }

    val bubbleShape = remember(message.isOwnMessage) {
        if (message.isOwnMessage) {
            RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 4.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            )
        }
    }

    val alignment = if (message.isOwnMessage) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (message.isOwnMessage) 48.dp else 6.dp,
                end = if (message.isOwnMessage) 6.dp else 48.dp,
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
            modifier = Modifier
                .widthIn(min = 80.dp, max = maxBubbleWidth)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongPress(message) }
                )
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = if (isGroupChat && !message.isOwnMessage && !message.isDeleted) 4.dp else 6.dp,
                    bottom = 5.dp
                )
            ) {
                // Sender name for group chats (other people's messages only)
                if (isGroupChat && !message.isOwnMessage && !message.isDeleted) {
                    val senderColor = remember(message.senderId) {
                        senderNameColor(message.senderId)
                    }
                    Text(
                        text = message.senderName ?: message.senderId.take(8),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = senderColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Forwarded label
                if (message.isForwarded && !message.isDeleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "\u21AA Forwarded",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = WhatsAppColors.ForwardedText,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Quoted reply card (above message content)
                if (!message.isDeleted && message.replyToMessageId != null && message.replyToSenderName != null) {
                    QuotedReplyCard(
                        senderName = message.replyToSenderName,
                        content = message.replyToContent,
                        type = message.replyToType,
                        thumbnailUrl = message.replyToMediaThumbnailUrl,
                        isOwnMessageQuote = message.isOwnMessage,
                        onClick = {
                            message.replyToMessageId.let(onQuotedReplyClick)
                        },
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (message.isDeleted) {
                    DeletedMessageContent(isOwnMessage = message.isOwnMessage)
                } else if (message.messageType == "location" && message.content != null) {
                    LocationContent(message = message)
                } else {
                    MessageContent(message = message)
                }
            }
        }
    }
}

@Composable
private fun MessageContent(
    message: MessageUi,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentText = message.content ?: ""
    val firstUrl = remember(contentText) { extractFirstUrl(contentText) }
    val linkColor = MaterialTheme.colorScheme.primary

    val annotatedText = remember(contentText, linkColor) {
        buildAnnotatedString {
            append(contentText)
            val urlPattern = Regex("""(https?://[^\s<>\"\)\]]+)""", RegexOption.IGNORE_CASE)
            for (match in urlPattern.findAll(contentText)) {
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = match.value,
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }

    Column(modifier = modifier) {
        ClickableText(
            text = annotatedText,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.5.sp,
                lineHeight = 20.sp
            ),
            onClick = { offset ->
                annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    }
            }
        )

        if (firstUrl != null) {
            LinkPreviewCard(
                url = firstUrl,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (message.isScheduled) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = "Scheduled",
                    tint = WhatsAppColors.MessageMeta,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            if (message.isStarred) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Starred",
                    tint = WhatsAppColors.StarColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            Text(
                text = message.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = WhatsAppColors.MessageMeta,
                fontSize = 11.sp,
                lineHeight = 11.sp
            )

            if (message.isOwnMessage) {
                Spacer(modifier = Modifier.width(3.dp))
                MessageStatusIcon(
                    status = message.status,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun LocationContent(
    message: MessageUi,
    modifier: Modifier = Modifier
) {
    val parts = message.content?.split(",")?.map { it.trim() }
    val lat = parts?.getOrNull(0)?.toDoubleOrNull()
    val lng = parts?.getOrNull(1)?.toDoubleOrNull()

    Column(modifier = modifier) {
        if (lat != null && lng != null) {
            LocationMessageBubble(
                latitude = lat,
                longitude = lng,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        } else {
            Text(
                text = message.content ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.5.sp,
                lineHeight = 20.sp
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = message.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = WhatsAppColors.MessageMeta,
                fontSize = 11.sp,
                lineHeight = 11.sp
            )

            if (message.isOwnMessage) {
                Spacer(modifier = Modifier.width(3.dp))
                MessageStatusIcon(
                    status = message.status,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun DeletedMessageContent(
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (isOwnMessage) "You deleted this message" else "\uD83D\uDEAB This message was deleted",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = WhatsAppColors.DeletedText,
            fontSize = 14.5.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private fun senderNameColor(senderId: String): Color {
    val index = (senderId.hashCode() and 0x7FFFFFFF) % SenderNameColors.size
    return SenderNameColors[index]
}
