package com.whatsappclone.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ChipBackground = Color(0xFFE1F2FA)
private val ChipBackgroundDark = Color(0xFF182229)
private val ChipTextColor = Color(0xFF54656F)
private val ChipTextColorDark = Color(0xFF8696A0)

@Composable
fun DateSeparator(
    text: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val backgroundColor = if (isDarkTheme) ChipBackgroundDark else ChipBackground
    val textColor = if (isDarkTheme) ChipTextColorDark else ChipTextColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
