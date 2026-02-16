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

    data class Error(
        val code: String,
        val message: String
    ) : ServerWsEvent()
}
