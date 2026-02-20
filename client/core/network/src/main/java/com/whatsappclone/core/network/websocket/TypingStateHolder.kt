package com.whatsappclone.core.network.websocket

import com.whatsappclone.core.common.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TypingStateHolder @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _typingUsers = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val typingUsers: StateFlow<Map<String, Set<String>>> = _typingUsers.asStateFlow()

    private val clearJobs = ConcurrentHashMap<Pair<String, String>, Job>()

    fun onTyping(chatId: String, userId: String, isTyping: Boolean) {
        val key = chatId to userId

        clearJobs.remove(key)?.cancel()

        if (isTyping) {
            addTypingUser(chatId, userId)

            clearJobs[key] = scope.launch {
                delay(Constants.TYPING_TIMEOUT_MS)
                removeTypingUser(chatId, userId)
                clearJobs.remove(key)
            }
        } else {
            removeTypingUser(chatId, userId)
        }
    }

    fun getTypingUsersForChat(chatId: String): Flow<Set<String>> {
        return _typingUsers.map { map -> map[chatId] ?: emptySet() }
    }

    fun clear() {
        clearJobs.values.forEach { it.cancel() }
        clearJobs.clear()
        _typingUsers.value = emptyMap()
    }

    private fun addTypingUser(chatId: String, userId: String) {
        _typingUsers.update { currentMap ->
            currentMap.toMutableMap().apply {
                val current = get(chatId) ?: emptySet()
                put(chatId, current + userId)
            }
        }
    }

    private fun removeTypingUser(chatId: String, userId: String) {
        _typingUsers.update { currentMap ->
            currentMap.toMutableMap().apply {
                val current = get(chatId) ?: return@apply
                val updated = current - userId
                if (updated.isEmpty()) {
                    remove(chatId)
                } else {
                    put(chatId, updated)
                }
            }
        }
    }
}
