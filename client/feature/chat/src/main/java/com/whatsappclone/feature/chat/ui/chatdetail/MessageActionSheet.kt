package com.whatsappclone.feature.chat.ui.chatdetail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsappclone.feature.chat.model.MessageUi

private val ActionIconTint = Color(0xFF54656F)
private val StarColor = Color(0xFFFFC107)
private val DeleteColor = Color(0xFFE53935)

enum class MessageAction {
    REACT, REPLY, FORWARD, COPY, STAR, INFO, DELETE, SELECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionSheet(
    message: MessageUi,
    onAction: (MessageAction) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Message preview at top
            MessagePreviewHeader(message = message)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Action grid — 2 columns
            val actions = buildList {
                if (!message.isDeleted) {
                    add(
                        ActionItem(
                            action = MessageAction.REACT,
                            icon = Icons.Filled.AddReaction,
                            label = "React",
                            tint = ActionIconTint
                        )
                    )
                }
                add(
                    ActionItem(
                        action = MessageAction.REPLY,
                        icon = Icons.AutoMirrored.Filled.Reply,
                        label = "Reply",
                        tint = ActionIconTint
                    )
                )
                add(
                    ActionItem(
                        action = MessageAction.FORWARD,
                        icon = Icons.Filled.Forward,
                        label = "Forward",
                        tint = ActionIconTint
                    )
                )
                if (message.messageType == "text" && !message.isDeleted) {
                    add(
                        ActionItem(
                            action = MessageAction.COPY,
                            icon = Icons.Filled.ContentCopy,
                            label = "Copy",
                            tint = ActionIconTint
                        )
                    )
                }
                add(
                    ActionItem(
                        action = MessageAction.STAR,
                        icon = if (message.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        label = if (message.isStarred) "Unstar" else "Star",
                        tint = if (message.isStarred) StarColor else ActionIconTint
                    )
                )
                if (message.isOwnMessage) {
                    add(
                        ActionItem(
                            action = MessageAction.INFO,
                            icon = Icons.Filled.Info,
                            label = "Info",
                            tint = ActionIconTint
                        )
                    )
                }
                add(
                    ActionItem(
                        action = MessageAction.DELETE,
                        icon = Icons.Filled.Delete,
                        label = "Delete",
                        tint = DeleteColor
                    )
                )
                add(
                    ActionItem(
                        action = MessageAction.SELECT,
                        icon = Icons.Filled.SelectAll,
                        label = "Select",
                        tint = ActionIconTint
                    )
                )
            }

            // Render actions in a 2-column grid
            val rows = actions.chunked(2)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowItems.forEach { item ->
                        ActionGridCell(
                            item = item,
                            onClick = {
                                onAction(item.action)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if odd number of items
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagePreviewHeader(
    message: MessageUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (message.isOwnMessage) "You" else (message.senderName ?: "Unknown"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (message.isDeleted) {
                Text(
                    text = "This message was deleted",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = message.content ?: when (message.messageType) {
                        "image" -> "\uD83D\uDCF7 Photo"
                        "video" -> "\uD83C\uDFA5 Video"
                        "audio" -> "\uD83C\uDFA4 Audio"
                        "document" -> "\uD83D\uDCC4 Document"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActionGridCell(
    item: ActionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = item.tint,
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (item.action == MessageAction.DELETE) {
                DeleteColor
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 12.sp
        )
    }
}

private data class ActionItem(
    val action: MessageAction,
    val icon: ImageVector,
    val label: String,
    val tint: Color
)
