package com.whatsappclone.feature.chat.ui.chatdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SystemBubbleColor = Color(0xFFE2F0FD)
private val SystemTextColor = Color(0xFF54656F)

/**
 * Renders a system / event message as a centered chip.
 *
 * Typical messages:
 * - "Alice created the group"
 * - "Bob added Charlie"
 * - "Dana left"
 * - "You changed the group description"
 */
@Composable
fun SystemMessageItem(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SystemBubbleColor,
            shadowElevation = 0.5.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                color = SystemTextColor,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
