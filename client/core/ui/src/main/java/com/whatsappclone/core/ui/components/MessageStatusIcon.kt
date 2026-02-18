package com.whatsappclone.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GrayStatus = Color(0xFF8696A0)
private val ReadBlue = Color(0xFF53BDEB)

@Composable
fun MessageStatusIcon(
    status: String,
    modifier: Modifier = Modifier
) {
    val iconSize = 16.dp

    when (status.lowercase()) {
        "pending" -> {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = "Pending",
                tint = GrayStatus,
                modifier = modifier.size(iconSize)
            )
        }
        "sending" -> {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Sending",
                tint = GrayStatus.copy(alpha = 0.5f),
                modifier = modifier.size(iconSize)
            )
        }
        "sent" -> {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Sent",
                tint = GrayStatus,
                modifier = modifier.size(iconSize)
            )
        }
        "delivered" -> {
            Icon(
                imageVector = Icons.Filled.DoneAll,
                contentDescription = "Delivered",
                tint = GrayStatus,
                modifier = modifier.size(iconSize)
            )
        }
        "read" -> {
            Icon(
                imageVector = Icons.Filled.DoneAll,
                contentDescription = "Read",
                tint = ReadBlue,
                modifier = modifier.size(iconSize)
            )
        }
    }
}
