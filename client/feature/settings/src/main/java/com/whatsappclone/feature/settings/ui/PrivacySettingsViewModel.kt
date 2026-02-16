package com.whatsappclone.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.feature.settings.data.PrivacyPreferences
import com.whatsappclone.feature.settings.data.PrivacyPreferencesStore
import com.whatsappclone.feature.settings.data.Visibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PrivacySettingsEvent {
    data object NavigateBack : PrivacySettingsEvent()
    data object NavigateToBlockedContacts : PrivacySettingsEvent()
}

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val privacyPreferencesStore: PrivacyPreferencesStore
) : ViewModel() {

    val preferences: StateFlow<PrivacyPreferences> = privacyPreferencesStore.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PrivacyPreferences()
        )

    private val _event = MutableSharedFlow<PrivacySettingsEvent>()
    val event: SharedFlow<PrivacySettingsEvent> = _event.asSharedFlow()

    fun onLastSeenChanged(visibility: Visibility) {
        viewModelScope.launch {
            privacyPreferencesStore.updateLastSeenVisibility(visibility)
        }
    }

    fun onProfilePhotoChanged(visibility: Visibility) {
        viewModelScope.launch {
            privacyPreferencesStore.updateProfilePhotoVisibility(visibility)
        }
    }

    fun onAboutChanged(visibility: Visibility) {
        viewModelScope.launch {
            privacyPreferencesStore.updateAboutVisibility(visibility)
        }
    }

    fun onReadReceiptsToggled(enabled: Boolean) {
        viewModelScope.launch {
            privacyPreferencesStore.updateReadReceipts(enabled)
        }
    }

    fun onGroupsChanged(visibility: Visibility) {
        viewModelScope.launch {
            privacyPreferencesStore.updateGroupsVisibility(visibility)
        }
    }

    fun onBlockedContactsClicked() {
        viewModelScope.launch {
            _event.emit(PrivacySettingsEvent.NavigateToBlockedContacts)
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            _event.emit(PrivacySettingsEvent.NavigateBack)
        }
    }
}
