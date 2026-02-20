package com.whatsappclone.feature.chat.call

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.whatsappclone.core.network.websocket.WebSocketManager
import com.whatsappclone.core.network.websocket.WsConnectionState
import com.whatsappclone.core.network.websocket.WsFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webSocketManager: WebSocketManager
) {
    companion object {
        private const val TAG = "CallService"
        private val STUN_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val eglBase: EglBase by lazy { EglBase.create() }

    private val _session = MutableStateFlow<CallSession?>(null)
    val session: StateFlow<CallSession?> = _session.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    private fun ensureFactory(): PeerConnectionFactory {
        peerConnectionFactory?.let { return it }

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        val factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()

        peerConnectionFactory = factory
        return factory
    }

    private fun createPeerConnection(): PeerConnection {
        val factory = ensureFactory()
        val rtcConfig = PeerConnection.RTCConfiguration(STUN_SERVERS).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        return factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val callId = _session.value?.callId ?: return
                val targetUserId = _session.value?.remoteUserId ?: return
                scope.launch {
                    sendIceCandidate(callId, targetUserId, candidate.sdp)
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d(TAG, "ICE connection state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        _session.update { it?.copy(state = CallState.CONNECTED) }
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        endCall("ice_failed")
                    }
                    else -> {}
                }
            }

            override fun onAddStream(stream: MediaStream) {
                stream.videoTracks?.firstOrNull()?.let { _remoteVideoTrack.value = it }
            }

            override fun onTrack(transceiver: org.webrtc.RtpTransceiver) {
                val track = transceiver.receiver.track()
                if (track is VideoTrack) {
                    _remoteVideoTrack.value = track
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(dc: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
        })!!.also { peerConnection = it }
    }

    private fun addLocalAudioTrack() {
        val factory = ensureFactory()
        val audioConstraints = MediaConstraints()
        val audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("audio_local", audioSource)
        localAudioTrack?.setEnabled(true)
        peerConnection?.addTrack(localAudioTrack, listOf("stream_local"))
    }

    // ── Outgoing call ────────────────────────────────────────────────────

    fun startCall(
        remoteUserId: String,
        remoteName: String,
        remoteAvatarUrl: String?,
        callType: String
    ) {
        if (_session.value != null) return

        val callId = UUID.randomUUID().toString()
        _session.value = CallSession(
            callId = callId,
            remoteUserId = remoteUserId,
            remoteName = remoteName,
            remoteAvatarUrl = remoteAvatarUrl,
            callType = callType,
            isOutgoing = true
        )

        createPeerConnection()
        addLocalAudioTrack()

        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                scope.launch {
                    sendCallOffer(callId, remoteUserId, sdp.description, callType)
                }
                _session.update { it?.copy(state = CallState.OUTGOING_RINGING) }
            }
        }, MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            if (callType == "video") {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
        })
    }

    // ── Incoming call ────────────────────────────────────────────────────

    fun onIncomingOffer(callId: String, callerId: String, callerName: String, callerAvatar: String?, sdp: String, callType: String) {
        if (_session.value != null) return

        _session.value = CallSession(
            callId = callId,
            remoteUserId = callerId,
            remoteName = callerName,
            remoteAvatarUrl = callerAvatar,
            callType = callType,
            isOutgoing = false,
            state = CallState.INCOMING_RINGING
        )

        createPeerConnection()
        addLocalAudioTrack()

        val remoteSdp = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), remoteSdp)

        for (candidate in pendingIceCandidates) {
            peerConnection?.addIceCandidate(candidate)
        }
        pendingIceCandidates.clear()
    }

    fun acceptCall() {
        val session = _session.value ?: return
        _session.update { it?.copy(state = CallState.CONNECTING) }

        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                scope.launch {
                    sendCallAnswer(session.callId, session.remoteUserId, sdp.description)
                }
            }
        }, MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            if (session.callType == "video") {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
        })
    }

    fun declineCall() {
        val session = _session.value ?: return
        endCall("declined")
        scope.launch {
            sendCallEnd(session.callId, session.remoteUserId, "declined")
        }
    }

    // ── Remote events ────────────────────────────────────────────────────

    fun onRemoteAnswer(sdp: String) {
        val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), remoteSdp)
        _session.update { it?.copy(state = CallState.CONNECTING) }
    }

    fun onRemoteIceCandidate(candidate: String) {
        val iceCandidate = IceCandidate("", 0, candidate)
        if (peerConnection?.remoteDescription != null) {
            peerConnection?.addIceCandidate(iceCandidate)
        } else {
            pendingIceCandidates.add(iceCandidate)
        }
    }

    fun onRemoteEnd(reason: String) {
        endCall(reason)
    }

    // ── Controls ─────────────────────────────────────────────────────────

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        localAudioTrack?.setEnabled(!newMuted)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeaker
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = newSpeaker
    }

    fun endCall(reason: String = "user_hangup") {
        val session = _session.value
        if (session != null && session.state != CallState.ENDED) {
            scope.launch {
                sendCallEnd(session.callId, session.remoteUserId, reason)
            }
        }

        _localVideoTrack.value?.dispose()
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        _isMuted.value = false
        _isSpeakerOn.value = false
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        pendingIceCandidates.clear()

        _session.update { it?.copy(state = CallState.ENDED) }
    }

    fun clearSession() {
        _session.value = null
    }

    // ── WebSocket signaling ──────────────────────────────────────────────

    private fun sendCallOffer(callId: String, targetUserId: String, sdp: String, callType: String) {
        if (webSocketManager.connectionState.value != WsConnectionState.CONNECTED) return
        val payload = buildJsonObject {
            put("call_id", JsonPrimitive(callId))
            put("target_user_id", JsonPrimitive(targetUserId))
            put("sdp", JsonPrimitive(sdp))
            put("call_type", JsonPrimitive(callType))
        }
        webSocketManager.send(WsFrame(event = "call.offer", data = payload))
    }

    private fun sendCallAnswer(callId: String, targetUserId: String, sdp: String) {
        if (webSocketManager.connectionState.value != WsConnectionState.CONNECTED) return
        val payload = buildJsonObject {
            put("call_id", JsonPrimitive(callId))
            put("target_user_id", JsonPrimitive(targetUserId))
            put("sdp", JsonPrimitive(sdp))
        }
        webSocketManager.send(WsFrame(event = "call.answer", data = payload))
    }

    private fun sendIceCandidate(callId: String, targetUserId: String, candidate: String) {
        if (webSocketManager.connectionState.value != WsConnectionState.CONNECTED) return
        val payload = buildJsonObject {
            put("call_id", JsonPrimitive(callId))
            put("target_user_id", JsonPrimitive(targetUserId))
            put("candidate", JsonPrimitive(candidate))
        }
        webSocketManager.send(WsFrame(event = "call.ice-candidate", data = payload))
    }

    private fun sendCallEnd(callId: String, targetUserId: String, reason: String) {
        if (webSocketManager.connectionState.value != WsConnectionState.CONNECTED) return
        val payload = buildJsonObject {
            put("call_id", JsonPrimitive(callId))
            put("target_user_id", JsonPrimitive(targetUserId))
            put("reason", JsonPrimitive(reason))
        }
        webSocketManager.send(WsFrame(event = "call.end", data = payload))
    }
}

/** Convenience base class to avoid implementing every SdpObserver method. */
private open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {
        Log.e("SdpObserver", "Create failed: $error")
    }
    override fun onSetFailure(error: String?) {
        Log.e("SdpObserver", "Set failed: $error")
    }
}
