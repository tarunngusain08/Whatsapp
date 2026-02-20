package com.whatsappclone.feature.contacts.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.relation.ContactWithUser
import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.chat.data.UserRepository
import com.whatsappclone.feature.contacts.data.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactPickerUiState(
    val contacts: List<ContactWithUser> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isCreatingChat: Boolean = false,
    val hasContactPermission: Boolean = false,
    val error: String? = null
) {
    /** Group contacts alphabetically by the first letter of deviceName. */
    val groupedContacts: Map<Char, List<ContactWithUser>>
        get() = contacts.groupBy { contact ->
            val firstChar = contact.deviceName.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar.isLetter()) firstChar else '#'
        }.toSortedMap()

    val sectionLetters: List<Char>
        get() = groupedContacts.keys.toList()
}

sealed class ContactPickerNavigationEvent {
    data class NavigateToChatDetail(val chatId: String) : ContactPickerNavigationEvent()
    data object NavigateToNewGroup : ContactPickerNavigationEvent()
    data object NavigateBack : ContactPickerNavigationEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _loadingState = MutableStateFlow(LoadingState())

    private val _navigationEvent = MutableSharedFlow<ContactPickerNavigationEvent>()
    val navigationEvent: SharedFlow<ContactPickerNavigationEvent> = _navigationEvent.asSharedFlow()

    private val currentUserId: String? = userRepository.getCurrentUserId()

    val uiState: StateFlow<ContactPickerUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                contactRepository.observeRegisteredContacts()
            } else {
                contactRepository.searchContacts(query)
            }
        },
        _searchQuery,
        _loadingState
    ) { contacts, query, loading ->
        ContactPickerUiState(
            contacts = contacts.filter { it.userId != currentUserId },
            searchQuery = query,
            isLoading = loading.isLoading,
            isSyncing = loading.isSyncing,
            isCreatingChat = loading.isCreatingChat,
            hasContactPermission = checkContactPermission(),
            error = loading.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactPickerUiState(isLoading = true)
    )

    init {
        if (checkContactPermission()) {
            syncContacts()
        }
    }

    fun onPermissionGranted() {
        syncContacts()
    }

    fun syncContacts() {
        viewModelScope.launch {
            _loadingState.update { it.copy(isSyncing = true, error = null) }
            when (val result = contactRepository.syncContacts()) {
                is AppResult.Success -> {
                    _loadingState.update { it.copy(isSyncing = false, isLoading = false) }
                }
                is AppResult.Error -> {
                    _loadingState.update {
                        it.copy(isSyncing = false, isLoading = false, error = result.message)
                    }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    private var serverSearchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        maybeSearchServer(query)
    }

    /**
     * When the query contains digits (likely a phone number), debounce
     * and query the backend to find registered users not in device contacts.
     */
    private fun maybeSearchServer(query: String) {
        serverSearchJob?.cancel()
        val digits = query.filter { it.isDigit() || it == '+' }
        if (digits.length < 4) return

        serverSearchJob = viewModelScope.launch {
            delay(600)
            contactRepository.searchByPhone(digits)
        }
    }

    fun onContactClicked(contact: ContactWithUser) {
        val userId = contact.userId ?: return
        if (_loadingState.value.isCreatingChat) return

        viewModelScope.launch {
            _loadingState.update { it.copy(isCreatingChat = true, error = null) }

            when (val result = chatRepository.createDirectChat(userId)) {
                is AppResult.Success -> {
                    _loadingState.update { it.copy(isCreatingChat = false) }
                    _navigationEvent.emit(
                        ContactPickerNavigationEvent.NavigateToChatDetail(result.data)
                    )
                }
                is AppResult.Error -> {
                    _loadingState.update {
                        it.copy(isCreatingChat = false, error = result.message)
                    }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun onNewGroupClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(ContactPickerNavigationEvent.NavigateToNewGroup)
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(ContactPickerNavigationEvent.NavigateBack)
        }
    }

    fun clearError() {
        _loadingState.update { it.copy(error = null) }
    }

    private fun checkContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private data class LoadingState(
        val isLoading: Boolean = false,
        val isSyncing: Boolean = false,
        val isCreatingChat: Boolean = false,
        val error: String? = null
    )
}
