package com.example.intelligentfileorganizer.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligentfileorganizer.data.repository.FileRepository
import com.example.intelligentfileorganizer.data.repository.StorageRepository
import com.example.intelligentfileorganizer.domain.model.FileItem
import com.example.intelligentfileorganizer.storage.IncomingFileHandler
import com.example.intelligentfileorganizer.storage.IncomingMetadata
import com.example.intelligentfileorganizer.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isStorageConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val pendingIncomingMetadata: IncomingMetadata? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val storageRepository: StorageRepository,
    private val fileRepository: FileRepository,
    private val incomingFileHandler: IncomingFileHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentFiles: StateFlow<List<FileItem>> = fileRepository.observeRecentFiles(limit = 10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshStorageStatus()
    }

    fun refreshStorageStatus() {
        viewModelScope.launch {
            val configured = storageRepository.verifyPermission()
            _uiState.update { it.copy(isStorageConfigured = configured) }
        }
    }

    fun handleIncomingUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = incomingFileHandler.validateAndProcess(uri)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingIncomingMetadata = result.data,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun clearPendingIncoming() {
        _uiState.update { it.copy(pendingIncomingMetadata = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

