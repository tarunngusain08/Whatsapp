package com.whatsappclone.feature.chat.ui.chatlist

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsappclone.core.ui.components.UnreadBadge
import com.whatsappclone.core.ui.components.UserAvatar
import com.whatsappclone.feature.chat.model.ChatItemUi

private val TypingGreen = Color(0xFF25D366)
private val MutedIcon = Color(0xFFBDBDBD)
private val PinnedBackground = Color(0xFFF0F4F0)

@Composable
fun ChatItemRow(
    chat: ChatItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    val isTyping = chat.typingUsers.isNotEmpty()
    val hasUnread = chat.unreadCount > 0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (chat.isPinned) Modifier.background(PinnedBackground.copy(alpha = 0.4f))
                    else Modifier
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                url = chat.avatarUrl,
                name = chat.name,
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (chat.isMuted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Muted",
                            modifier = Modifier.size(14.dp),
                            tint = MutedIcon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                if (isTyping) {
                    TypingText(users = chat.typingUsers, chatType = chat.chatType)
                } else {
                    LastMessagePreview(
                        senderName = chat.lastMessageSenderName,
                        preview = chat.lastMessagePreview,
                        chatType = chat.chatType,
                        hasUnread = hasUnread
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = chat.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 12.sp,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(45f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    UnreadBadge(count = chat.unreadCount)
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun TypingText(
    users: Set<String>,
    chatType: String,
    modifier: Modifier = Modifier
) {
    val typingText = when {
        chatType == "group" && users.size == 1 -> "${users.first()} is typing..."
        chatType == "group" && users.size == 2 -> "${users.joinToString(", ")} are typing..."
        chatType == "group" && users.size > 2 -> "Several people are typing..."
        else -> "typing..."
    }

    Text(
        text = typingText,
        style = MaterialTheme.typography.bodyMedium,
        color = TypingGreen,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun LastMessagePreview(
    senderName: String?,
    preview: String?,
    chatType: String,
    hasUnread: Boolean,
    modifier: Modifier = Modifier
) {
    val displayText = buildString {
        if (chatType == "group" && senderName != null) {
            append("$senderName: ")
        }
        append(preview ?: "")
    }

    Text(
        text = displayText.ifEmpty { "No messages yet" },
        style = MaterialTheme.typography.bodyMedium,
        color = if (hasUnread) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        fontSize = 14.sp,
        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
