package com.whatsappclone.feature.group.ui.info

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.GroupDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import com.whatsappclone.core.network.api.ChatApi
import com.whatsappclone.core.network.model.dto.AddParticipantsRequest
import com.whatsappclone.core.network.model.dto.CreateChatRequest
import com.whatsappclone.core.network.model.dto.UpdateRoleRequest
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.core.network.model.safeApiCallUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class GroupMember(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
    val isOnline: Boolean
) {
    val isAdmin: Boolean get() = role == "admin"
}

data class GroupInfoUiState(
    val chatId: String = "",
    val groupName: String = "",
    val groupDescription: String? = null,
    val groupAvatarUrl: String? = null,
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Long = 0L,
    val memberCount: Int = 0,
    val members: List<GroupMember> = emptyList(),
    val isMuted: Boolean = false,
    val isAdminOnly: Boolean = false,
    val currentUserId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isCurrentUserAdmin: Boolean
        get() = members.any { it.userId == currentUserId && it.isAdmin }

    val formattedCreatedDate: String
        get() {
            if (createdAt == 0L) return ""
            val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(createdAt))
        }
}

sealed class GroupInfoEvent {
    data object NavigateBack : GroupInfoEvent()
    data class ShowError(val message: String) : GroupInfoEvent()
    data class ShowSuccess(val message: String) : GroupInfoEvent()
}

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupDao: GroupDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val chatApi: ChatApi,
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) : ViewModel() {

    private val chatId: String = savedStateHandle.get<String>("chatId") ?: ""
    private val currentUserId: String = encryptedPrefs.getString(KEY_CURRENT_USER_ID, "") ?: ""

    private val _localState = MutableStateFlow(
        GroupInfoUiState(chatId = chatId, currentUserId = currentUserId)
    )

    private val _eventChannel = Channel<GroupInfoEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    val uiState: StateFlow<GroupInfoUiState> = combine(
        _localState,
        groupDao.observeGroup(chatId),
        chatParticipantDao.observeParticipants(chatId),
        chatDao.observeChat(chatId)
    ) { localState, group, participants, chat ->
        val members = participants.mapNotNull { participant ->
            val user = userDao.getById(participant.userId)
            user?.let {
                GroupMember(
                    userId = it.id,
                    displayName = it.displayName,
                    avatarUrl = it.avatarUrl,
                    role = participant.role,
                    isOnline = it.isOnline
                )
            }
        }.sortedWith(
            compareByDescending<GroupMember> { it.isAdmin }
                .thenBy { it.displayName.lowercase() }
        )

        val creatorName = if (group?.createdBy != null) {
            if (group.createdBy == currentUserId) "You"
            else userDao.getById(group.createdBy)?.displayName ?: "Unknown"
        } else ""

        localState.copy(
            groupName = group?.name ?: localState.groupName,
            groupDescription = group?.description ?: localState.groupDescription,
            groupAvatarUrl = group?.avatarUrl ?: localState.groupAvatarUrl,
            createdBy = group?.createdBy ?: localState.createdBy,
            createdByName = creatorName,
            createdAt = group?.createdAt ?: localState.createdAt,
            memberCount = members.size,
            members = members,
            isMuted = chat?.isMuted ?: false,
            isAdminOnly = group?.isAdminOnly ?: false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupInfoUiState(chatId = chatId, currentUserId = currentUserId)
    )

    // ── Actions ──────────────────────────────────────────────────────────

    fun toggleAdminOnly() {
        val currentState = uiState.value
        if (!currentState.isCurrentUserAdmin) return

        val newAdminOnly = !currentState.isAdminOnly

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }

            try {
                val existingGroup = groupDao.getByChatId(chatId)
                if (existingGroup != null) {
                    groupDao.upsert(existingGroup.copy(isAdminOnly = newAdminOnly))
                }
                _localState.update { it.copy(isLoading = false) }

                val status = if (newAdminOnly) "enabled" else "disabled"
                _eventChannel.send(
                    GroupInfoEvent.ShowSuccess("Admin-only messaging $status")
                )
            } catch (e: Exception) {
                _localState.update {
                    it.copy(isLoading = false, error = e.message)
                }
                _eventChannel.send(
                    GroupInfoEvent.ShowError(e.message ?: "Failed to update setting")
                )
            }
        }
    }

    fun toggleMute() {
        val currentMuted = uiState.value.isMuted
        viewModelScope.launch {
            chatDao.setMuted(chatId, !currentMuted, updatedAt = System.currentTimeMillis())
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }

            val result = safeApiCallUnit {
                chatApi.removeParticipant(chatId, userId)
            }

            when (result) {
                is AppResult.Success -> {
                    chatParticipantDao.delete(chatId, userId)
                    _localState.update { it.copy(isLoading = false) }
                    _eventChannel.send(GroupInfoEvent.ShowSuccess("Member removed"))
                }
                is AppResult.Error -> {
                    _localState.update { it.copy(isLoading = false, error = result.message) }
                    _eventChannel.send(GroupInfoEvent.ShowError(result.message))
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun toggleAdminRole(userId: String) {
        val member = uiState.value.members.find { it.userId == userId } ?: return
        val newRole = if (member.isAdmin) "member" else "admin"

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }

            val result = safeApiCallUnit {
                chatApi.updateRole(chatId, userId, UpdateRoleRequest(role = newRole))
            }

            when (result) {
                is AppResult.Success -> {
                    chatParticipantDao.upsert(
                        ChatParticipantEntity(
                            chatId = chatId,
                            userId = userId,
                            role = newRole,
                            joinedAt = System.currentTimeMillis()
                        )
                    )
                    _localState.update { it.copy(isLoading = false) }
                    val action = if (newRole == "admin") "promoted to admin" else "demoted to member"
                    _eventChannel.send(
                        GroupInfoEvent.ShowSuccess("${member.displayName} $action")
                    )
                }
                is AppResult.Error -> {
                    _localState.update { it.copy(isLoading = false, error = result.message) }
                    _eventChannel.send(GroupInfoEvent.ShowError(result.message))
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun updateGroupName(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }

            val result = safeApiCall {
                chatApi.updateChat(chatId, CreateChatRequest(type = "group", participantIds = emptyList(), name = name))
            }

            when (result) {
                is AppResult.Success -> {
                    val group = groupDao.getByChatId(chatId)
                    if (group != null) {
                        groupDao.upsert(group.copy(name = name, updatedAt = System.currentTimeMillis()))
                    }
                    _localState.update { it.copy(isLoading = false) }
                    _eventChannel.send(GroupInfoEvent.ShowSuccess("Group name updated"))
                }
                is AppResult.Error -> {
                    _localState.update { it.copy(isLoading = false, error = result.message) }
                    _eventChannel.send(GroupInfoEvent.ShowError(result.message))
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun updateGroupDescription(description: String) {
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }

            val result = safeApiCall {
                chatApi.updateChat(
                    chatId,
                    CreateChatRequest(type = "group", participantIds = emptyList(), name = uiState.value.groupName)
                )
            }

            when (result) {
                is AppResult.Success -> {
                    val group = groupDao.getByChatId(chatId)
                    if (group != null) {
                        groupDao.upsert(group.copy(description = description.ifBlank { null }, updatedAt = System.currentTimeMillis()))
                    }
                    _localState.update { it.copy(isLoading = false) }
                    _eventChannel.send(GroupInfoEvent.ShowSuccess("Group description updated"))
                }
                is AppResult.Error -> {
                    _localState.update { it.copy(isLoading = false, error = result.message) }
                    _eventChannel.send(GroupInfoEvent.ShowError(result.message))
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun addParticipants(userIds: List<String>) {
        if (userIds.isEmpty()) return

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }

            val result = safeApiCallUnit {
                chatApi.addParticipants(chatId, AddParticipantsRequest(userIds = userIds))
            }

            when (result) {
                is AppResult.Success -> {
                    val newParticipants = userIds.map { id ->
                        ChatParticipantEntity(
                            chatId = chatId,
                            userId = id,
                            role = "member",
                            joinedAt = System.currentTimeMillis()
                        )
                    }
                    chatParticipantDao.upsertAll(newParticipants)
                    _localState.update { it.copy(isLoading = false) }
                    _eventChannel.send(
                        GroupInfoEvent.ShowSuccess("${userIds.size} participant(s) added")
                    )
                }
                is AppResult.Error -> {
                    _localState.update { it.copy(isLoading = false, error = result.message) }
                    _eventChannel.send(GroupInfoEvent.ShowError(result.message))
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }

            val result = safeApiCallUnit {
                chatApi.removeParticipant(chatId, currentUserId)
            }

            when (result) {
                is AppResult.Success -> {
                    chatParticipantDao.delete(chatId, currentUserId)
                    _localState.update { it.copy(isLoading = false) }
                    _eventChannel.send(GroupInfoEvent.NavigateBack)
                }
                is AppResult.Error -> {
                    _localState.update { it.copy(isLoading = false, error = result.message) }
                    _eventChannel.send(GroupInfoEvent.ShowError(result.message))
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun deleteAndExit() {
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }

            val result = safeApiCallUnit {
                chatApi.removeParticipant(chatId, currentUserId)
            }

            when (result) {
                is AppResult.Success -> {
                    chatParticipantDao.deleteAllForChat(chatId)
                    _localState.update { it.copy(isLoading = false) }
                    _eventChannel.send(GroupInfoEvent.NavigateBack)
                }
                is AppResult.Error -> {
                    _localState.update { it.copy(isLoading = false, error = result.message) }
                    _eventChannel.send(GroupInfoEvent.ShowError(result.message))
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun loadNonMemberUsers(onResult: (List<SelectableUser>) -> Unit) {
        viewModelScope.launch {
            val currentParticipants = chatParticipantDao.getParticipants(chatId)
            val memberIds = currentParticipants.map { it.userId }.toSet()

            userDao.observeAllUsers().collect { allUsers ->
                val nonMembers = allUsers
                    .filter { it.id !in memberIds }
                    .map { user ->
                        SelectableUser(
                            userId = user.id,
                            displayName = user.displayName,
                            avatarUrl = user.avatarUrl,
                            phone = user.phone
                        )
                    }
                onResult(nonMembers)
            }
        }
    }

    fun clearError() {
        _localState.update { it.copy(error = null) }
    }

    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}
