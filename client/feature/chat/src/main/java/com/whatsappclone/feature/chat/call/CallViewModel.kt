package com.whatsappclone.feature.chat.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val callService: CallService
) : ViewModel() {

    private val calleeName: String = savedStateHandle["calleeName"] ?: "Unknown"
    private val calleeAvatar: String? = savedStateHandle.get<String>("avatarUrl")
        ?.takeIf { it.isNotBlank() }
    private val callType: String = savedStateHandle["callType"] ?: "audio"
    private val calleeUserId: String? = savedStateHandle["calleeUserId"]
    private val incomingCallId: String? = savedStateHandle["incomingCallId"]

    val session: StateFlow<CallSession?> = callService.session
    val isMuted: StateFlow<Boolean> = callService.isMuted
    val isSpeakerOn: StateFlow<Boolean> = callService.isSpeakerOn

    val callState: StateFlow<CallState> = callService.session
        .map { it?.state ?: CallState.IDLE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CallState.IDLE)

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()
    private var timerJob: Job? = null

    init {
        if (incomingCallId == null && calleeUserId != null) {
            callService.startCall(calleeUserId, calleeName, calleeAvatar, callType)
        }

        viewModelScope.launch {
            callState.collect { state ->
                if (state == CallState.CONNECTED) {
                    startTimer()
                } else {
                    timerJob?.cancel()
                    timerJob = null
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (callState.value == CallState.CONNECTED) {
                delay(1000)
                _elapsedSeconds.value++
            }
        }
    }

    fun toggleMute() = callService.toggleMute()
    fun toggleSpeaker() = callService.toggleSpeaker()

    fun acceptCall() = callService.acceptCall()
    fun declineCall() {
        callService.declineCall()
    }

    fun endCall() {
        callService.endCall()
    }

    override fun onCleared() {
        super.onCleared()
        if (callState.value != CallState.ENDED && callState.value != CallState.IDLE) {
            callService.endCall()
        }
    }
}
