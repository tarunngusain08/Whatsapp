package com.whatsappclone.feature.chat.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.network.api.StatusApi
import com.whatsappclone.core.network.model.dto.CreateStatusRequest
import com.whatsappclone.core.network.model.dto.StatusDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusContact(
    val userId: String,
    val statuses: List<StatusDto>,
    val hasUnviewed: Boolean
)

data class StatusListUiState(
    val myStatuses: List<StatusDto> = emptyList(),
    val contactStatuses: List<StatusContact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusApi: StatusApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusListUiState(isLoading = true))
    val uiState: StateFlow<StatusListUiState> = _uiState.asStateFlow()

    init {
        loadStatuses()
    }

    fun loadStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val myResponse = statusApi.getMyStatuses()
                val contactResponse = statusApi.getContactStatuses()

                val myStatuses = if (myResponse.isSuccessful) {
                    myResponse.body()?.data ?: emptyList()
                } else emptyList()

                val allContactStatuses = if (contactResponse.isSuccessful) {
                    contactResponse.body()?.data ?: emptyList()
                } else emptyList()

                val grouped = allContactStatuses.groupBy { it.userId }.map { (userId, statuses) ->
                    StatusContact(
                        userId = userId,
                        statuses = statuses.sortedBy { it.createdAt },
                        hasUnviewed = statuses.any { it.viewers.isEmpty() }
                    )
                }

                _uiState.update {
                    it.copy(
                        myStatuses = myStatuses,
                        contactStatuses = grouped,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load statuses")
                }
            }
        }
    }

    fun createTextStatus(text: String, bgColor: String) {
        viewModelScope.launch {
            try {
                statusApi.createStatus(
                    CreateStatusRequest(type = "text", content = text, bgColor = bgColor)
                )
                loadStatuses()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to post status") }
            }
        }
    }

    fun createImageStatus(imageUrl: String, caption: String?) {
        viewModelScope.launch {
            try {
                statusApi.createStatus(
                    CreateStatusRequest(type = "image", content = imageUrl, caption = caption)
                )
                loadStatuses()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to post status") }
            }
        }
    }

    fun markViewed(statusId: String) {
        viewModelScope.launch {
            try {
                statusApi.viewStatus(statusId)
            } catch (_: Exception) { }
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            try {
                statusApi.deleteStatus(statusId)
                loadStatuses()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete status") }
            }
        }
    }

    fun getStatusesForUser(userId: String): List<StatusDto> {
        val state = _uiState.value
        return if (userId == "me") {
            state.myStatuses
        } else {
            state.contactStatuses.find { it.userId == userId }?.statuses ?: emptyList()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
