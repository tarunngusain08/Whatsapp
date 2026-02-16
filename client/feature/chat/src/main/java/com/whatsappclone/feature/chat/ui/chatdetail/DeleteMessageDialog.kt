package com.whatsappclone.feature.chat.ui.chatdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsappclone.feature.chat.model.MessageUi

private const val ONE_HOUR_MS = 60 * 60 * 1000L

@Composable
fun DeleteMessageDialog(
    message: MessageUi,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canDeleteForEveryone = message.isOwnMessage &&
        (System.currentTimeMillis() - message.timestamp) < ONE_HOUR_MS

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = "Delete message?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                if (canDeleteForEveryone) {
                    Text(
                        text = "You can delete this message for everyone or just for yourself.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "This message will be deleted from this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Column {
                if (canDeleteForEveryone) {
                    TextButton(
                        onClick = {
                            onDeleteForEveryone()
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "Delete for everyone",
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }

                TextButton(
                    onClick = {
                        onDeleteForMe()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Delete for me",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }
        }
    )
}
