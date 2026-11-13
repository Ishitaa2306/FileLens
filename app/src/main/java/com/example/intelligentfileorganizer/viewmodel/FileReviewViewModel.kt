package com.example.intelligentfileorganizer.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligentfileorganizer.storage.IncomingMetadata
import com.example.intelligentfileorganizer.storage.PdfPreviewGenerator
import com.example.intelligentfileorganizer.util.FileNameUtils
import com.example.intelligentfileorganizer.util.MimeTypeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileReviewUiState(
    val metadata: IncomingMetadata? = null,
    val editableNameWithoutExtension: String = "",
    val extension: String = "",
    val pdfThumbnail: Bitmap? = null,
    val errorMessage: String? = null
)

class FileReviewViewModel(
    private val pdfPreviewGenerator: PdfPreviewGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileReviewUiState())
    val uiState: StateFlow<FileReviewUiState> = _uiState.asStateFlow()

    fun setIncomingMetadata(metadata: IncomingMetadata) {
        val baseName = FileNameUtils.stripExtension(metadata.displayName)
        _uiState.update {
            it.copy(
                metadata = metadata,
                editableNameWithoutExtension = baseName,
                extension = metadata.extension,
                pdfThumbnail = null,
                errorMessage = null
            )
        }

        if (metadata.mimeType == MimeTypeUtils.MIME_PDF || metadata.extension.equals("pdf", true)) {
            viewModelScope.launch {
                val thumb = pdfPreviewGenerator.generateFirstPageThumbnail(metadata.uri)
                _uiState.update { it.copy(pdfThumbnail = thumb) }
            }
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
