package com.whatsappclone.feature.chat.ui.chatlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.whatsappclone.core.ui.theme.WhatsAppColors
import com.whatsappclone.feature.chat.model.ChatItemUi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItemRow(
    chat: ChatItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onPinChat: () -> Unit = {},
    onMuteChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onArchiveChat: () -> Unit = {}
) {
    val isTyping = chat.typingUsers.isNotEmpty()
    val hasUnread = chat.unreadCount > 0
    var showContextMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (chat.isPinned) Modifier.background(WhatsAppColors.PinnedBackground.copy(alpha = 0.4f))
                    else Modifier
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                UserAvatar(
                    url = chat.avatarUrl,
                    name = chat.name,
                    size = 52.dp
                )
                if (chat.isOnline && chat.chatType == "direct") {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .padding(2.dp)
                            .background(
                                color = WhatsAppColors.OnlineGreen,
                                shape = CircleShape
                            )
                    )
                }
            }

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
                            imageVector = Icons.Filled.NotificationsOff,
                            contentDescription = "Muted",
                            modifier = Modifier.size(14.dp),
                            tint = WhatsAppColors.MutedIcon
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

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (chat.isPinned) "Unpin" else "Pin") },
                    onClick = {
                        showContextMenu = false
                        onPinChat()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pin chat",
                            modifier = Modifier.size(20.dp).rotate(45f)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (chat.isMuted) "Unmute" else "Mute") },
                    onClick = {
                        showContextMenu = false
                        onMuteChat()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (chat.isMuted) Icons.Filled.NotificationsOff else Icons.Filled.Notifications,
                            contentDescription = "Mute notifications",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Archive") },
                    onClick = {
                        showContextMenu = false
                        onArchiveChat()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Archive,
                            contentDescription = "Archive chat",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onDeleteChat()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete chat",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
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
        color = WhatsAppColors.TypingGreen,
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
