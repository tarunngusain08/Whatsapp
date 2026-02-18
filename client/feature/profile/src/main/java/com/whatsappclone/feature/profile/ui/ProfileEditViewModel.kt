package com.whatsappclone.feature.profile.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.core.network.api.UserApi
import com.whatsappclone.core.network.model.dto.UpdateProfileRequest
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.core.network.url.BaseUrlProvider
import com.whatsappclone.feature.media.data.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditUiState(
    val userId: String = "",
    val displayName: String = "",
    val statusText: String = "",
    val phone: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val mediaRepository: MediaRepository,
    private val baseUrlProvider: BaseUrlProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { userApi.getMe() }) {
                is AppResult.Success -> {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            userId = user.id,
                            displayName = user.displayName,
                            statusText = user.statusText ?: "",
                            phone = user.phone,
                            avatarUrl = user.avatarUrl,
                            isLoading = false
                        )
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    fun onDisplayNameChanged(name: String) {
        if (name.length <= Constants.MAX_DISPLAY_NAME_LENGTH) {
            _uiState.update { it.copy(displayName = name, saveSuccess = false) }
        }
    }

    fun onStatusTextChanged(status: String) {
        if (status.length <= Constants.MAX_STATUS_TEXT_LENGTH) {
            _uiState.update { it.copy(statusText = status, saveSuccess = false) }
        }
    }

    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true) }

            val uploaderId = _uiState.value.userId
            when (val result = mediaRepository.uploadImage(uri, uploaderId)) {
                is AppResult.Success -> {
                    val mediaId = result.data.mediaId
                    val base = baseUrlProvider.getBaseUrl().trimEnd('/')
                    val newAvatarUrl = "$base/media/$mediaId/download"

                    when (val updateResult = safeApiCall {
                        userApi.updateProfile(
                            UpdateProfileRequest(avatarUrl = newAvatarUrl)
                        )
                    }) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    avatarUrl = newAvatarUrl,
                                    isUploadingAvatar = false
                                )
                            }
                            // Also update local cache
                            val existing = userDao.getById(uploaderId)
                            if (existing != null) {
                                userDao.upsert(
                                    existing.copy(
                                        avatarUrl = newAvatarUrl,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }

                        is AppResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isUploadingAvatar = false,
                                    error = updateResult.message
                                )
                            }
                        }

                        is AppResult.Loading -> Unit
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            error = result.message
                        )
                    }
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        val trimmedName = state.displayName.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(error = "Display name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            when (val result = safeApiCall {
                userApi.updateProfile(
                    UpdateProfileRequest(
                        displayName = trimmedName,
                        statusText = state.statusText.trim().ifBlank { null }
                    )
                )
            }) {
                is AppResult.Success -> {
                    val updatedUser = result.data
                    // Update local cache
                    val existing = userDao.getById(state.userId)
                    if (existing != null) {
                        userDao.upsert(
                            existing.copy(
                                displayName = updatedUser.displayName,
                                statusText = updatedUser.statusText,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    _uiState.update {
                        it.copy(isSaving = false, saveSuccess = true)
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, error = result.message)
                    }
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
