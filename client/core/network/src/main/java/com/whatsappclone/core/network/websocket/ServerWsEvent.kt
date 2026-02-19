package com.whatsappclone.core.network.websocket

sealed class ServerWsEvent {

    data class NewMessage(
        val chatId: String,
        val messageJson: String
    ) : ServerWsEvent()

    data class MessageSent(
        val clientMsgId: String,
        val messageId: String,
        val chatId: String,
        val timestamp: String
    ) : ServerWsEvent()

    data class MessageStatus(
        val messageId: String,
        val chatId: String,
        val status: String,
        val userId: String
    ) : ServerWsEvent()

    data class MessageDeleted(
        val messageId: String,
        val chatId: String,
        val deletedForEveryone: Boolean
    ) : ServerWsEvent()

    data class TypingEvent(
        val chatId: String,
        val userId: String,
        val isTyping: Boolean
    ) : ServerWsEvent()

    data class PresenceEvent(
        val userId: String,
        val online: Boolean,
        val lastSeen: String?
    ) : ServerWsEvent()

    data class ChatCreated(
        val chatJson: String
    ) : ServerWsEvent()

    data class ChatUpdated(
        val chatId: String,
        val updateJson: String
    ) : ServerWsEvent()

    data class GroupMemberAdded(
        val chatId: String,
        val userId: String,
        val addedBy: String
    ) : ServerWsEvent()

    data class GroupMemberRemoved(
        val chatId: String,
        val userId: String,
        val removedBy: String
    ) : ServerWsEvent()

    data class MessageReaction(
        val messageId: String,
        val chatId: String,
        val userId: String,
        val emoji: String,
        val removed: Boolean
    ) : ServerWsEvent()

    data class CallOffer(
        val callId: String,
        val callerId: String,
        val sdp: String,
        val callType: String
    ) : ServerWsEvent()

    data class CallAnswer(
        val callId: String,
        val answererId: String,
        val sdp: String
    ) : ServerWsEvent()

    data class CallIceCandidate(
        val callId: String,
        val senderId: String,
        val candidate: String
    ) : ServerWsEvent()

    data class CallEnd(
        val callId: String,
        val senderId: String,
        val reason: String
    ) : ServerWsEvent()

    data class Error(
        val code: String,
        val message: String
    ) : ServerWsEvent()
}
