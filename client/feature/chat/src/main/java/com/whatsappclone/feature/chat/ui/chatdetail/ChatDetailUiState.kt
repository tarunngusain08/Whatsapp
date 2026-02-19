package com.whatsappclone.feature.chat.ui.chatdetail

data class ChatDetailUiState(
    val chatId: String = "",
    val chatName: String = "",
    val chatAvatarUrl: String? = null,
    val chatType: String = "direct",
    val otherUserId: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: String? = null,
    val typingUsers: Set<String> = emptySet(),
    val composerText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMuted: Boolean = false,

    // Admin-only messaging state
    val isAdminOnlyChat: Boolean = false,
    val isCurrentUserAdmin: Boolean = false,

    // Disappearing messages
    val disappearingTimer: String = "off",

    // In-chat search state
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val matchedMessageIds: List<String> = emptyList(),
    val currentMatchIndex: Int = 0,

    // Multi-select state
    val isSelectionMode: Boolean = false,
    val selectedMessageIds: Set<String> = emptySet()
) {
    val isComposerDisabled: Boolean
        get() = isAdminOnlyChat && !isCurrentUserAdmin && chatType == "group"

    val canSend: Boolean get() = composerText.isNotBlank() && !isComposerDisabled

    val selectedCount: Int get() = selectedMessageIds.size

    val totalSearchMatches: Int get() = matchedMessageIds.size

    val currentMatchMessageId: String?
        get() = matchedMessageIds.getOrNull(currentMatchIndex)

    val subtitleText: String
        get() = when {
            typingUsers.isNotEmpty() -> {
                val names = typingUsers.toList()
                when {
                    names.size == 1 -> "${names[0]} is typing..."
                    names.size == 2 -> "${names.joinToString(" and ")} are typing..."
                    else -> "Several people are typing..."
                }
            }
            isOnline -> "online"
            lastSeen != null -> lastSeen
            else -> ""
        }

    val isSubtitleHighlighted: Boolean
        get() = typingUsers.isNotEmpty() || isOnline
}
