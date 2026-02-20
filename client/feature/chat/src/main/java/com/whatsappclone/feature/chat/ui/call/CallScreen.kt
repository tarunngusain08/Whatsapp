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
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsappclone.core.ui.components.UserAvatar
import com.whatsappclone.feature.chat.call.CallState
import com.whatsappclone.feature.chat.call.CallViewModel

@Composable
fun CallScreen(
    calleeName: String,
    calleeAvatarUrl: String?,
    callType: String,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CallViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()

    LaunchedEffect(callState) {
        if (callState == CallState.ENDED) {
            viewModel.callService.clearSession()
            onEndCall()
        }
    }

    val formattedTime = remember(elapsedSeconds) {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        "%02d:%02d".format(minutes, seconds)
    }

    val displayName = session?.remoteName ?: calleeName
    val displayAvatar = session?.remoteAvatarUrl ?: calleeAvatarUrl
    val displayCallType = session?.callType ?: callType
    val isIncoming = session?.isOutgoing == false && callState == CallState.INCOMING_RINGING

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
                url = displayAvatar,
                name = displayName,
                size = 120.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (callState) {
                    CallState.CONNECTED ->
                        "${displayCallType.replaceFirstChar { it.uppercase() }} call · $formattedTime"
                    CallState.CONNECTING -> "Connecting…"
                    CallState.INCOMING_RINGING -> "Incoming ${displayCallType} call"
                    CallState.OUTGOING_RINGING -> "Calling…"
                    CallState.ENDED -> "Call ended"
                    CallState.IDLE -> "Initialising…"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isIncoming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline
                    FloatingActionButton(
                        onClick = {
                            viewModel.declineCall()
                            onEndCall()
                        },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        containerColor = Color(0xFFEB5545)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Accept
                    FloatingActionButton(
                        onClick = { viewModel.acceptCall() },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        containerColor = Color(0xFF25D366)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else {
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
                        onClick = { viewModel.toggleMute() }
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
                        onClick = { viewModel.toggleSpeaker() }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                FloatingActionButton(
                    onClick = {
                        viewModel.endCall()
                    },
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
