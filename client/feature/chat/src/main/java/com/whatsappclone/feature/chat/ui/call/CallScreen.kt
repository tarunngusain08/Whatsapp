package com.whatsappclone.feature.chat.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsappclone.core.ui.components.UserAvatar
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    calleeName: String,
    calleeAvatarUrl: String?,
    callType: String,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        isConnected = true
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    val formattedTime = remember(elapsedSeconds) {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        "%02d:%02d".format(minutes, seconds)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B2733))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            UserAvatar(
                url = calleeAvatarUrl,
                name = calleeName,
                size = 120.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = calleeName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isConnected) {
                    "${callType.replaceFirstChar { it.uppercase() }} call · $formattedTime"
                } else {
                    "Calling…"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallActionButton(
                    icon = {
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = if (isMuted) "Unmute" else "Mute",
                    isActive = isMuted,
                    onClick = { isMuted = !isMuted }
                )

                CallActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Speaker",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = "Speaker",
                    isActive = isSpeakerOn,
                    onClick = { isSpeakerOn = !isSpeakerOn }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            FloatingActionButton(
                onClick = onEndCall,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                containerColor = Color(0xFFEB5545)
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "End call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun CallActionButton(
    icon: @Composable () -> Unit,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (isActive) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}
