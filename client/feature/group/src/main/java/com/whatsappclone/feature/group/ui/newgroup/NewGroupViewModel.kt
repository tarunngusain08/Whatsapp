package com.whatsappclone.feature.group.ui.newgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.feature.group.domain.CreateGroupUseCase
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

data class ContactItem(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val phone: String
)

data class NewGroupUiState(
    val allContacts: List<ContactItem> = emptyList(),
    val filteredContacts: List<ContactItem> = emptyList(),
    val selectedContacts: List<ContactItem> = emptyList(),
    val searchQuery: String = "",
    val groupName: String = "",
    val groupDescription: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canProceedToSetup: Boolean
        get() = selectedContacts.isNotEmpty()

    val canCreateGroup: Boolean
        get() = groupName.isNotBlank() && selectedContacts.isNotEmpty() && !isLoading

    val selectedCount: Int
        get() = selectedContacts.size
}

sealed class NewGroupEvent {
    data class NavigateToSetup(val selectedIds: List<String>) : NewGroupEvent()
    data class NavigateToChatDetail(val chatId: String) : NewGroupEvent()
    data class ShowError(val message: String) : NewGroupEvent()
}

@HiltViewModel
class NewGroupViewModel @Inject constructor(
    private val userDao: UserDao,
    private val createGroupUseCase: CreateGroupUseCase
) : ViewModel() {

    private val _internalState = MutableStateFlow(NewGroupUiState())

    private val _eventChannel = Channel<NewGroupEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    val uiState: StateFlow<NewGroupUiState> = combine(
        _internalState,
        userDao.observeAllUsers()
    ) { state, users ->
        val contacts = users.map { it.toContactItem() }
        val filtered = if (state.searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.displayName.contains(state.searchQuery, ignoreCase = true) ||
                        contact.phone.contains(state.searchQuery, ignoreCase = true)
            }
        }
        state.copy(
            allContacts = contacts,
            filteredContacts = filtered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NewGroupUiState()
    )

    // ── Step 1: Contact Selection ─────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _internalState.update { it.copy(searchQuery = query) }
    }

    fun toggleContact(contact: ContactItem) {
        _internalState.update { state ->
            val isSelected = state.selectedContacts.any { it.userId == contact.userId }
            val updated = if (isSelected) {
                state.selectedContacts.filter { it.userId != contact.userId }
            } else {
                if (state.selectedContacts.size >= Constants.MAX_GROUP_MEMBERS) {
                    viewModelScope.launch {
                        _eventChannel.send(
                            NewGroupEvent.ShowError(
                                "Maximum ${Constants.MAX_GROUP_MEMBERS} members allowed"
                            )
                        )
                    }
                    return@update state
                }
                state.selectedContacts + contact
            }
            state.copy(selectedContacts = updated)
        }
    }

    fun removeSelectedContact(contact: ContactItem) {
        _internalState.update { state ->
            state.copy(
                selectedContacts = state.selectedContacts.filter { it.userId != contact.userId }
            )
        }
    }

    fun onProceedToSetup() {
        val state = _internalState.value
        if (state.selectedContacts.isNotEmpty()) {
            viewModelScope.launch {
                _eventChannel.send(
                    NewGroupEvent.NavigateToSetup(
                        state.selectedContacts.map { it.userId }
                    )
                )
            }
        }
    }

    // ── Step 2: Group Setup ───────────────────────────────────────────────

    fun onGroupNameChanged(name: String) {
        if (name.length <= Constants.MAX_GROUP_NAME_LENGTH) {
            _internalState.update { it.copy(groupName = name) }
        }
    }

    fun onGroupDescriptionChanged(description: String) {
        if (description.length <= Constants.MAX_GROUP_DESCRIPTION_LENGTH) {
            _internalState.update { it.copy(groupDescription = description) }
        }
    }

    fun onCreateGroup() {
        val state = _internalState.value
        if (!state.canCreateGroup) return

        viewModelScope.launch {
            _internalState.update { it.copy(isLoading = true, error = null) }

            val result = createGroupUseCase(
                name = state.groupName,
                participantIds = state.selectedContacts.map { it.userId }
            )

            when (result) {
                is AppResult.Success -> {
                    _internalState.update { it.copy(isLoading = false) }
                    _eventChannel.send(NewGroupEvent.NavigateToChatDetail(result.data))
                }

                is AppResult.Error -> {
                    _internalState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    _eventChannel.send(NewGroupEvent.ShowError(result.message))
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _internalState.update { it.copy(error = null) }
    }

    private fun UserEntity.toContactItem() = ContactItem(
        userId = id,
        displayName = displayName,
        avatarUrl = avatarUrl,
        phone = phone
    )
}
