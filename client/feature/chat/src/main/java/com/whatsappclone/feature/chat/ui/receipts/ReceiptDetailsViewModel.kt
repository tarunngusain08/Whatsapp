package com.whatsappclone.feature.chat.ui.receipts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.network.api.MessageApi
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.core.common.result.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptItem(
    val userId: String,
    val displayName: String?,
    val status: String,
    val updatedAt: String
)

data class ReceiptDetailsUiState(
    val messageId: String = "",
    val deliveredReceipts: List<ReceiptItem> = emptyList(),
    val readReceipts: List<ReceiptItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReceiptDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageApi: MessageApi,
    private val userDao: UserDao
) : ViewModel() {

    private val messageId: String = checkNotNull(savedStateHandle["messageId"])

    private val _uiState = MutableStateFlow(ReceiptDetailsUiState(messageId = messageId, isLoading = true))
    val uiState: StateFlow<ReceiptDetailsUiState> = _uiState.asStateFlow()

    init {
        loadReceipts()
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            when (val result = safeApiCall { messageApi.getMessageReceipts(messageId) }) {
                is AppResult.Success -> {
                    val receipts = result.data.map { dto ->
                        val user = userDao.getById(dto.userId)
                        ReceiptItem(
                            userId = dto.userId,
                            displayName = user?.displayName,
                            status = dto.status,
                            updatedAt = dto.updatedAt
                        )
                    }
                    _uiState.update {
                        it.copy(
                            deliveredReceipts = receipts.filter { r -> r.status == "delivered" },
                            readReceipts = receipts.filter { r -> r.status == "read" },
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
