package com.whatsappclone.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val WhatsAppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object BubbleShapes {
    val SentBubble = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 20.dp,
        bottomStart = 12.dp,
        bottomEnd = 4.dp
    )
    val ReceivedBubble = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 12.dp,
        bottomStart = 4.dp,
        bottomEnd = 12.dp
    )
    val SentBubbleFirst = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 20.dp,
        bottomStart = 12.dp,
        bottomEnd = 4.dp
    )
    val ReceivedBubbleFirst = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 12.dp,
        bottomStart = 4.dp,
        bottomEnd = 12.dp
    )
}

object CardShapes {
    val Default = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val Dialog = RoundedCornerShape(28.dp)
}
