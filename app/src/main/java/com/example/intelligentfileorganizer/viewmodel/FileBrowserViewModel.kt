package com.example.intelligentfileorganizer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligentfileorganizer.data.repository.FileRepository
import com.example.intelligentfileorganizer.data.repository.FolderRepository
import com.example.intelligentfileorganizer.domain.model.FileItem
import com.example.intelligentfileorganizer.domain.model.FolderItem
import com.example.intelligentfileorganizer.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileBrowserUiState(
    val currentFolder: FolderItem? = null,
    val subfolders: List<FolderItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val breadcrumbPath: String = "Documents / Intelligent File Organizer",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FileBrowserViewModel(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private val folderHistory = mutableListOf<Long>()

    init {
        loadRoot()
    }

    fun loadRoot() {
        folderHistory.clear()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            folderRepository.observeRootFolders().collect { folders ->
                _uiState.update {
                    it.copy(
                        currentFolder = null,
                        subfolders = folders,
                        files = emptyList(),
                        breadcrumbPath = "Documents / Intelligent File Organizer",
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun openFolder(folder: FolderItem) {
        folderHistory.add(folder.id)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentFolder = folder) }
            val path = folderRepository.getReadablePath(folder.id)

            launch {
                folderRepository.observeSubfolders(folder.id).collect { subfolders ->
                    _uiState.update { it.copy(subfolders = subfolders) }
                }
            }

            launch {
                fileRepository.observeFilesByFolder(folder.id).collect { files ->
                    _uiState.update {
                        it.copy(
                            files = files,
                            breadcrumbPath = path,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun navigateUp(): Boolean {
        if (folderHistory.isEmpty()) {
            return false
        }
        folderHistory.removeAt(folderHistory.lastIndex)
        if (folderHistory.isEmpty()) {
            loadRoot()
        } else {
            val parentId = folderHistory.last()
            viewModelScope.launch {
                val parentFolder = folderRepository.getFolderById(parentId)
                if (parentFolder != null) {
                    openFolder(parentFolder)
                } else {
                    loadRoot()
                }
            }
        }
        return true
    }

    fun renameFile(fileId: Long, newName: String) {
        viewModelScope.launch {
            when (val result = fileRepository.renameFile(fileId, newName)) {
                is AppResult.Success -> {
                    // List will auto-update via Room Flow
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch {
            fileRepository.deleteFile(fileId)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

