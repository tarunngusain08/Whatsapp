package com.whatsappclone.feature.media.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.database.entity.MediaEntity
import com.whatsappclone.feature.media.data.MediaRepository
import com.whatsappclone.feature.media.util.DownloadState
import com.whatsappclone.feature.media.util.MediaDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MediaViewerUiState(
    val mediaEntity: MediaEntity? = null,
    val localFile: File? = null,
    val isLoading: Boolean = true,
    val downloadProgress: Float = 0f,
    val error: String? = null,
    val senderName: String = "",
    val timestamp: String = ""
)

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val downloadManager: MediaDownloadManager
) : ViewModel() {

    private val mediaId: String = checkNotNull(savedStateHandle["mediaId"])
    val senderName: String = savedStateHandle["senderName"] ?: ""
    val timestamp: String = savedStateHandle["timestamp"] ?: ""

    private val _uiState = MutableStateFlow(
        MediaViewerUiState(
            senderName = senderName,
            timestamp = timestamp
        )
    )
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            val entity = mediaRepository.getMedia(mediaId)
            if (entity == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Media not found")
                }
                return@launch
            }

            _uiState.update { it.copy(mediaEntity = entity) }

            // Check if we already have a local file
            val existingLocal = entity.localPath?.let { File(it) }
            if (existingLocal != null && existingLocal.exists()) {
                _uiState.update {
                    it.copy(localFile = existingLocal, isLoading = false)
                }
                return@launch
            }

            // Need to download
            downloadManager.enqueueDownload(mediaId, entity.storageUrl)
            observeDownload()
        }
    }

    private fun observeDownload() {
        viewModelScope.launch {
            downloadManager.observeDownloadState(mediaId).collect { stateMap ->
                when (val state = stateMap[mediaId]) {
                    is DownloadState.Downloading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = true,
                                downloadProgress = state.progress
                            )
                        }
                    }

                    is DownloadState.Completed -> {
                        _uiState.update {
                            it.copy(
                                localFile = state.file,
                                isLoading = false,
                                downloadProgress = 1f
                            )
                        }
                    }

                    is DownloadState.Failed -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = state.message
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun retry() {
        _uiState.update { it.copy(error = null, isLoading = true) }
        loadMedia()
    }
}
