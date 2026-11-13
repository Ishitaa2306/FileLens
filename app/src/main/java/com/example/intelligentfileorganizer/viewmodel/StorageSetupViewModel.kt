package com.example.intelligentfileorganizer.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligentfileorganizer.data.repository.StorageRepository
import com.example.intelligentfileorganizer.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StorageSetupUiState(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedUriString: String? = null
)

class StorageSetupViewModel(
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageSetupUiState())
    val uiState: StateFlow<StorageSetupUiState> = _uiState.asStateFlow()

    init {
        checkCurrentConfiguration()
    }

    private fun checkCurrentConfiguration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val config = storageRepository.getStorageConfig()
            if (config != null && config.isConfigured) {
                val isValid = storageRepository.verifyPermission()
                _uiState.update {
                    it.copy(
                        isConfigured = isValid,
                        isLoading = false,
                        selectedUriString = config.treeUri,
                        errorMessage = if (!isValid) "Stored folder permission has expired. Please reselect your Documents folder." else null
                    )
                }
            } else {
                _uiState.update { it.copy(isConfigured = false, isLoading = false) }
            }
        }
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = storageRepository.setupStorage(uri)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isConfigured = true,
                            isLoading = false,
                            selectedUriString = result.data.treeUri,
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

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

