package com.whatsappclone.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.feature.settings.data.NotificationPreferences
import com.whatsappclone.feature.settings.data.NotificationPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NotificationSettingsEvent {
    data object NavigateBack : NotificationSettingsEvent()
}

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val store: NotificationPreferencesStore
) : ViewModel() {

    val preferences: StateFlow<NotificationPreferences> = store.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationPreferences()
        )

    private val _event = MutableSharedFlow<NotificationSettingsEvent>()
    val event: SharedFlow<NotificationSettingsEvent> = _event.asSharedFlow()

    fun onBackClicked() {
        viewModelScope.launch {
            _event.emit(NotificationSettingsEvent.NavigateBack)
        }
    }

    // ── Message notifications ─────────────────────────────────────────────

    fun onMessageNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch { store.updateMessageNotifications(enabled) }
    }

    fun onMessageToneChanged(tone: String) {
        viewModelScope.launch { store.updateMessageTone(tone) }
    }

    fun onMessageVibrateChanged(vibrate: String) {
        viewModelScope.launch { store.updateMessageVibrate(vibrate) }
    }

    // ── Group notifications ───────────────────────────────────────────────

    fun onGroupNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch { store.updateGroupNotifications(enabled) }
    }

    fun onGroupToneChanged(tone: String) {
        viewModelScope.launch { store.updateGroupTone(tone) }
    }

    fun onGroupVibrateChanged(vibrate: String) {
        viewModelScope.launch { store.updateGroupVibrate(vibrate) }
    }

    // ── In-app notifications ──────────────────────────────────────────────

    fun onInAppSoundsToggled(enabled: Boolean) {
        viewModelScope.launch { store.updateInAppSounds(enabled) }
    }

    fun onInAppPreviewToggled(enabled: Boolean) {
        viewModelScope.launch { store.updateInAppPreview(enabled) }
    }
}
