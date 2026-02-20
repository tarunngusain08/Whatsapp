package com.whatsappclone.core.network.websocket

import android.util.Log
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.network.token.TokenManager
import com.whatsappclone.core.network.url.BaseUrlProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val baseUrlProvider: BaseUrlProvider,
    private val json: Json
) {

    companion object {
        private const val TAG = "WebSocketManager"
        private const val CLOSE_NORMAL = 1000
        private const val CLOSE_GOING_AWAY = 1001
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO + exceptionHandler)

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<ServerWsEvent>(
        replay = 0,
        extraBufferCapacity = 512,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ServerWsEvent> = _events.asSharedFlow()

    private var webSocket: WebSocket? = null
    private val connectionMutex = Mutex()

    private var heartbeatJob: Job? = null
    private var pongTimeoutJob: Job? = null
    private var reconnectJob: Job? = null
    @Volatile private var awaitingPong = false

    @Volatile private var reconnectAttempt = 0
    @Volatile private var intentionalDisconnect = false

    // ── Public API ──────────────────────────────────────────────────────

    fun connect() {
        scope.launch {
            connectionMutex.withLock {
                if (_connectionState.value == WsConnectionState.CONNECTED ||
                    _connectionState.value == WsConnectionState.CONNECTING
                ) {
                    Log.d(TAG, "Already connected or connecting, ignoring connect()")
                    return@launch
                }
                intentionalDisconnect = false
                doConnect()
            }
        }
    }

    fun disconnect() {
        scope.launch {
            connectionMutex.withLock {
                intentionalDisconnect = true
                cancelReconnect()
                cancelHeartbeat()
                closeSocket(CLOSE_NORMAL, "Client disconnect")
                _connectionState.value = WsConnectionState.DISCONNECTED
                Log.d(TAG, "Disconnected intentionally")
            }
        }
    }

    fun send(frame: WsFrame): Boolean {
        val socket = webSocket ?: return false
        return try {
            val text = json.encodeToString(WsFrame.serializer(), frame)
            socket.send(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send frame: ${frame.event}", e)
            false
        }
    }

    fun sendTyping(chatId: String, isTyping: Boolean) {
        val data = kotlinx.serialization.json.buildJsonObject {
            put("chat_id", kotlinx.serialization.json.JsonPrimitive(chatId))
        }
        val eventName = if (isTyping) "typing.start" else "typing.stop"
        send(WsFrame(event = eventName, data = data))
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }

    // ── Connection ──────────────────────────────────────────────────────

    private fun doConnect() {
        try {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrBlank()) {
                Log.w(TAG, "No access token available, cannot connect")
                _connectionState.value = WsConnectionState.DISCONNECTED
                return
            }

            val wsUrl = baseUrlProvider.getWsUrl()

            _connectionState.value = WsConnectionState.CONNECTING
            Log.d(TAG, "Connecting to WebSocket...")

            val request = Request.Builder()
                .url(wsUrl)
                .header("Authorization", "Bearer $token")
                .build()

            webSocket = okHttpClient.newWebSocket(request, createListener())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate WebSocket connection", e)
            _connectionState.value = WsConnectionState.DISCONNECTED
        }
    }

    private fun createListener(): WebSocketListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened")
            _connectionState.value = WsConnectionState.CONNECTED
            reconnectAttempt = 0
            awaitingPong = false
            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleTextMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: code=$code reason=$reason")
            if (code == 4401) {
                scope.launch {
                    Log.d(TAG, "WS close 4401 — refreshing token before reconnect")
                    tokenManager.refreshToken()
                }
            }
            webSocket.close(CLOSE_NORMAL, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: code=$code reason=$reason")
            cancelHeartbeat()
            if (!intentionalDisconnect) {
                scheduleReconnect()
            } else {
                _connectionState.value = WsConnectionState.DISCONNECTED
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}", t)
            cancelHeartbeat()
            closeSocketSilently()
            if (!intentionalDisconnect) {
                val httpCode = response?.code
                if (httpCode == 401) {
                    scope.launch {
                        Log.d(TAG, "WS 401 — attempting token refresh before reconnect")
                        val refreshed = tokenManager.refreshToken()
                        if (refreshed) {
                            scheduleReconnect()
                        } else {
                            Log.e(TAG, "Token refresh failed, not reconnecting")
                            _connectionState.value = WsConnectionState.DISCONNECTED
                        }
                    }
                } else {
                    scheduleReconnect()
                }
            } else {
                _connectionState.value = WsConnectionState.DISCONNECTED
            }
        }
    }

    // ── Heartbeat ───────────────────────────────────────────────────────

    private fun startHeartbeat() {
        cancelHeartbeat()
        heartbeatJob = scope.launch {
            while (true) {
                delay(Constants.WS_HEARTBEAT_INTERVAL_MS)
                if (_connectionState.value != WsConnectionState.CONNECTED) break

                awaitingPong = true
                val sent = send(WsFrame(event = "ping"))
                if (!sent) {
                    Log.w(TAG, "Failed to send ping, triggering reconnect")
                    scheduleReconnect()
                    break
                }

                pongTimeoutJob = scope.launch {
                    delay(Constants.WS_PONG_TIMEOUT_MS)
                    if (awaitingPong) {
                        Log.w(TAG, "Pong timeout, triggering reconnect")
                        cancelHeartbeat()
                        closeSocketSilently()
                        scheduleReconnect()
                    }
                }
            }
        }
    }

    private fun cancelHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        pongTimeoutJob?.cancel()
        pongTimeoutJob = null
        awaitingPong = false
    }

    // ── Reconnect ───────────────────────────────────────────────────────

    private fun scheduleReconnect() {
        if (intentionalDisconnect) return
        if (reconnectJob?.isActive == true) return

        _connectionState.value = WsConnectionState.RECONNECTING

        reconnectJob = scope.launch {
            val delayMs = calculateReconnectDelay()
            Log.d(TAG, "Reconnecting in ${delayMs}ms (attempt ${reconnectAttempt + 1})")
            delay(delayMs)
            reconnectAttempt++
            connectionMutex.withLock {
                if (intentionalDisconnect) return@launch
                doConnect()
            }
        }
    }

    private fun calculateReconnectDelay(): Long {
        val exponentialDelay = INITIAL_RECONNECT_DELAY_MS * (1L shl reconnectAttempt.coerceAtMost(5))
        return exponentialDelay.coerceAtMost(Constants.WS_MAX_RECONNECT_DELAY_MS)
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempt = 0
    }

    // ── Message Parsing ─────────────────────────────────────────────────

    private fun handleTextMessage(text: String) {
        try {
            val frame = json.decodeFromString(WsFrame.serializer(), text)

            if (frame.event == "pong") {
                awaitingPong = false
                pongTimeoutJob?.cancel()
                return
            }

            val event = parseServerEvent(frame)
            if (event != null) {
                val emitted = _events.tryEmit(event)
                if (!emitted) {
                    Log.w(TAG, "Event buffer full, dropping event: ${frame.event}")
                }
            } else {
                Log.w(TAG, "Unknown WS event: ${frame.event}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WS message: $text", e)
        }
    }

    private fun parseServerEvent(frame: WsFrame): ServerWsEvent? {
        val data = frame.data?.jsonObject ?: return null

        return when (frame.event) {
            "message.new" -> ServerWsEvent.NewMessage(
                chatId = data.string("chat_id"),
                messageJson = data.toString()
            )

            "message.sent" -> ServerWsEvent.MessageSent(
                clientMsgId = data.string("client_msg_id"),
                messageId = data.string("message_id"),
                chatId = data.string("chat_id"),
                timestamp = data.string("timestamp")
            )

            "message.status" -> ServerWsEvent.MessageStatus(
                messageId = data.string("message_id"),
                chatId = data.string("chat_id"),
                status = data.string("status"),
                userId = data.string("user_id")
            )

            "message.deleted" -> ServerWsEvent.MessageDeleted(
                messageId = data.string("message_id"),
                chatId = data.string("chat_id"),
                deletedForEveryone = data["deleted_for_everyone"]?.jsonPrimitive?.booleanOrNull ?: false
            )

            "typing" -> ServerWsEvent.TypingEvent(
                chatId = data.string("chat_id"),
                userId = data.string("user_id"),
                isTyping = data["typing"]?.jsonPrimitive?.boolean
                    ?: data["is_typing"]?.jsonPrimitive?.boolean
                    ?: true
            )

            "presence" -> ServerWsEvent.PresenceEvent(
                userId = data.string("user_id"),
                online = data["online"]?.jsonPrimitive?.boolean ?: false,
                lastSeen = data.stringOrNull("last_seen")
            )

            "chat.created" -> ServerWsEvent.ChatCreated(
                chatJson = data.toString()
            )

            "chat.updated" -> ServerWsEvent.ChatUpdated(
                chatId = data.string("chat_id"),
                updateJson = data.toString()
            )

            "group.member.added" -> ServerWsEvent.GroupMemberAdded(
                chatId = data.string("chat_id"),
                userId = data.string("user_id"),
                addedBy = data.string("added_by")
            )

            "group.member.removed" -> ServerWsEvent.GroupMemberRemoved(
                chatId = data.string("chat_id"),
                userId = data.string("user_id"),
                removedBy = data.string("removed_by")
            )

            "message.reaction" -> ServerWsEvent.MessageReaction(
                messageId = data.string("message_id"),
                chatId = data.string("chat_id"),
                userId = data.string("user_id"),
                emoji = data.stringOrNull("emoji") ?: "",
                removed = data["removed"]?.jsonPrimitive?.booleanOrNull ?: false
            )

            "call.offer" -> ServerWsEvent.CallOffer(
                callId = data.string("call_id"),
                callerId = data.string("caller_id"),
                sdp = data.string("sdp"),
                callType = data.stringOrNull("call_type") ?: "audio"
            )

            "call.answer" -> ServerWsEvent.CallAnswer(
                callId = data.string("call_id"),
                answererId = data.string("answerer_id"),
                sdp = data.string("sdp")
            )

            "call.ice-candidate" -> ServerWsEvent.CallIceCandidate(
                callId = data.string("call_id"),
                senderId = data.string("sender_id"),
                candidate = data.string("candidate")
            )

            "call.end" -> ServerWsEvent.CallEnd(
                callId = data.string("call_id"),
                senderId = data.string("sender_id"),
                reason = data.stringOrNull("reason") ?: "hangup"
            )

            "error" -> ServerWsEvent.Error(
                code = data.stringOrNull("code") ?: "UNKNOWN",
                message = data.stringOrNull("message") ?: "Unknown error"
            )

            else -> null
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun closeSocket(code: Int, reason: String?) {
        try {
            webSocket?.close(code, reason)
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebSocket", e)
        }
        webSocket = null
    }

    private fun closeSocketSilently() {
        try {
            webSocket?.cancel()
        } catch (_: Exception) {
        }
        webSocket = null
    }

    private fun JsonObject.string(key: String): String =
        get(key)?.jsonPrimitive?.content ?: ""

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.jsonPrimitive?.content
}
