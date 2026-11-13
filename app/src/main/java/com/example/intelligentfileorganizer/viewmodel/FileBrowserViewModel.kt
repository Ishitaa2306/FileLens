package com.example.intelligentfileorganizer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligentfileorganizer.data.repository.FileRepository
import com.example.intelligentfileorganizer.data.repository.FolderRepository
import com.example.intelligentfileorganizer.data.repository.StorageRepository
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
    val filteredSubfolders: List<FolderItem> = emptyList(),
    val filteredFiles: List<FileItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFileForDetails: FileItem? = null,
    val breadcrumbPath: String = "Documents / Intelligent File Organizer",
    val isLoading: Boolean = false,
    val isPermissionLost: Boolean = false,
    val errorMessage: String? = null
)

class FileBrowserViewModel(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private val folderHistory = mutableListOf<Long>()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        viewModelScope.launch {
            val hasPermission = storageRepository.verifyPermission()
            if (!hasPermission) {
                _uiState.update {
                    it.copy(
                        isPermissionLost = true,
                        errorMessage = "Access to the selected Documents folder has been revoked or expired. Please reconfigure storage."
                    )
                }
            } else {
                _uiState.update { it.copy(isPermissionLost = false) }
                loadRoot()
            }
        }
    }

    fun loadRoot() {
        folderHistory.clear()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, searchQuery = "") }
            folderRepository.observeRootFolders().collect { folders ->
                _uiState.update { current ->
                    current.copy(
                        currentFolder = null,
                        subfolders = folders,
                        files = emptyList(),
                        filteredSubfolders = filterFolders(folders, current.searchQuery),
                        filteredFiles = emptyList(),
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
            _uiState.update { it.copy(isLoading = true, currentFolder = folder, searchQuery = "") }
            val path = folderRepository.getReadablePath(folder.id)

            launch {
                folderRepository.observeSubfolders(folder.id).collect { subfolders ->
                    _uiState.update { current ->
                        current.copy(
                            subfolders = subfolders,
                            filteredSubfolders = filterFolders(subfolders, current.searchQuery)
                        )
                    }
                }
            }

            launch {
                fileRepository.observeFilesByFolder(folder.id).collect { files ->
                    _uiState.update { current ->
                        current.copy(
                            files = files,
                            filteredFiles = filterFiles(files, current.searchQuery),
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

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredSubfolders = filterFolders(current.subfolders, query),
                filteredFiles = filterFiles(current.files, query)
            )
        }
    }

    fun selectFileForDetails(file: FileItem?) {
        _uiState.update { it.copy(selectedFileForDetails = file) }
    }

    fun renameFile(fileId: Long, newName: String) {
        viewModelScope.launch {
            when (val result = fileRepository.renameFile(fileId, newName)) {
                is AppResult.Success -> {
                    // Update details dialog if currently selected
                    if (_uiState.value.selectedFileForDetails?.id == fileId) {
                        _uiState.update { it.copy(selectedFileForDetails = result.data) }
                    }
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
            if (_uiState.value.selectedFileForDetails?.id == fileId) {
                _uiState.update { it.copy(selectedFileForDetails = null) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun filterFolders(list: List<FolderItem>, query: String): List<FolderItem> {
        if (query.isBlank()) return list
        return list.filter { it.name.contains(query, ignoreCase = true) }
    }

    private fun filterFiles(list: List<FileItem>, query: String): List<FileItem> {
        if (query.isBlank()) return list
        return list.filter { it.storedName.contains(query, ignoreCase = true) || it.extension.contains(query, ignoreCase = true) }
    }
}
