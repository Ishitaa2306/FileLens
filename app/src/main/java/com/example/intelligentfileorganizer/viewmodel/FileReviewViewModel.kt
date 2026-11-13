package com.example.intelligentfileorganizer.viewmodel

import androidx.lifecycle.ViewModel
import com.example.intelligentfileorganizer.storage.IncomingMetadata
import com.example.intelligentfileorganizer.util.FileNameUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FileReviewUiState(
    val metadata: IncomingMetadata? = null,
    val editableNameWithoutExtension: String = "",
    val extension: String = "",
    val errorMessage: String? = null
)

class FileReviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FileReviewUiState())
    val uiState: StateFlow<FileReviewUiState> = _uiState.asStateFlow()

    fun setIncomingMetadata(metadata: IncomingMetadata) {
        val baseName = FileNameUtils.stripExtension(metadata.displayName)
        _uiState.update {
            it.copy(
                metadata = metadata,
                editableNameWithoutExtension = baseName,
                extension = metadata.extension,
                errorMessage = null
            )
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update {
            it.copy(
                editableNameWithoutExtension = newName,
                errorMessage = if (newName.isBlank()) "Filename cannot be empty" else null
            )
        }
    }

    fun getFinalFileName(): String {
        val current = _uiState.value
        val base = current.editableNameWithoutExtension.trim().ifEmpty { "file" }
        return FileNameUtils.ensureExtension(base, current.extension)
    }
}

