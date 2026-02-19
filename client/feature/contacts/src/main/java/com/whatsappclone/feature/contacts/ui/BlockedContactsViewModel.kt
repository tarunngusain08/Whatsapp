package com.whatsappclone.feature.contacts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.feature.chat.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockedContactsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val userRepository: UserRepository
) : ViewModel() {

    val blockedUsers: StateFlow<List<UserEntity>> = userDao.observeBlockedUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            userRepository.unblockUser(userId)
        }
    }
}
