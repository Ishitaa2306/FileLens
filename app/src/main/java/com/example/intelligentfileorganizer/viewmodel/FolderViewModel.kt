package com.example.intelligentfileorganizer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligentfileorganizer.data.repository.FileRepository
import com.example.intelligentfileorganizer.data.repository.FolderRepository
import com.example.intelligentfileorganizer.domain.model.FileItem
import com.example.intelligentfileorganizer.domain.model.FolderItem
import com.example.intelligentfileorganizer.storage.IncomingMetadata
import com.example.intelligentfileorganizer.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FolderSelectionUiState(
    val currentFolder: FolderItem? = null,
    val foldersList: List<FolderItem> = emptyList(),
    val breadcrumbPath: String = "Documents / Intelligent File Organizer",
    val isCreatingFolder: Boolean = false,
    val isSavingFile: Boolean = false,
    val savedFile: FileItem? = null,
    val errorMessage: String? = null
)

class FolderViewModel(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderSelectionUiState())
    val uiState: StateFlow<FolderSelectionUiState> = _uiState.asStateFlow()

    private val folderHistory = mutableListOf<Long>()

    init {
        loadRootFolders()
    }

    fun loadRootFolders() {
        folderHistory.clear()
        viewModelScope.launch {
            folderRepository.observeRootFolders().collect { folders ->
                _uiState.update {
                    it.copy(
                        currentFolder = null,
                        foldersList = folders,
                        breadcrumbPath = "Documents / Intelligent File Organizer",
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun openFolder(folder: FolderItem) {
        folderHistory.add(folder.id)
        viewModelScope.launch {
            val path = folderRepository.getReadablePath(folder.id)
            folderRepository.observeSubfolders(folder.id).collect { subfolders ->
                _uiState.update {
                    it.copy(
                        currentFolder = folder,
                        foldersList = subfolders,
                        breadcrumbPath = path,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun navigateUp(): Boolean {
        if (folderHistory.isEmpty()) {
            return false // Already at root
        }
        folderHistory.removeAt(folderHistory.lastIndex)
        if (folderHistory.isEmpty()) {
            loadRootFolders()
        } else {
            val parentId = folderHistory.last()
            viewModelScope.launch {
                val parentFolder = folderRepository.getFolderById(parentId)
                if (parentFolder != null) {
                    val path = folderRepository.getReadablePath(parentId)
                    folderRepository.observeSubfolders(parentId).collect { subfolders ->
                        _uiState.update {
                            it.copy(
                                currentFolder = parentFolder,
                                foldersList = subfolders,
                                breadcrumbPath = path,
                                errorMessage = null
                            )
                        }
                    }
                } else {
                    loadRootFolders()
                }
            }
        }
        return true
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingFolder = true, errorMessage = null) }
            val currentParentId = _uiState.value.currentFolder?.id
            when (val result = folderRepository.createFolder(name, currentParentId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isCreatingFolder = false) }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreatingFolder = false,
                            errorMessage = result.message
                        )
                    }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun saveFileToCurrentFolder(metadata: IncomingMetadata, desiredFileName: String) {
        val targetFolder = _uiState.value.currentFolder
        if (targetFolder == null) {
            _uiState.update { it.copy(errorMessage = "Please create or select a folder first to save your file.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingFile = true, errorMessage = null) }
            val result = fileRepository.saveIncomingFile(
                metadata = metadata,
                desiredFileName = desiredFileName,
                targetFolderId = targetFolder.id
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSavingFile = false,
                            savedFile = result.data,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingFile = false,
                            errorMessage = result.message
                        )
                    }
                }
                AppResult.Loading -> {}
            }
        }
    }

    fun clearSavedFile() {
        _uiState.update { it.copy(savedFile = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

