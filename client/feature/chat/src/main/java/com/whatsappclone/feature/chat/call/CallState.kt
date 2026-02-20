package com.whatsappclone.feature.chat.call

enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTING,
    CONNECTED,
    ENDED
}

data class CallSession(
    val callId: String,
    val remoteUserId: String,
    val remoteName: String,
    val remoteAvatarUrl: String?,
    val callType: String,
    val isOutgoing: Boolean,
    val state: CallState = if (isOutgoing) CallState.OUTGOING_RINGING else CallState.INCOMING_RINGING
)
