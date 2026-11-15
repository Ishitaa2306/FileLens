package com.example.intelligentfileorganizer.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligentfileorganizer.domain.analyzer.DocumentAnalysisStatus
import com.example.intelligentfileorganizer.domain.analyzer.DocumentAnalyzer
import com.example.intelligentfileorganizer.domain.analyzer.DocumentAnalysisResult
import com.example.intelligentfileorganizer.domain.analyzer.DocumentExtractionSource
import com.example.intelligentfileorganizer.storage.IncomingMetadata
import com.example.intelligentfileorganizer.storage.PdfPreviewGenerator
import com.example.intelligentfileorganizer.util.FileNameUtils
import com.example.intelligentfileorganizer.util.MimeTypeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OcrUiState {
    data object Idle : OcrUiState
    data class Processing(val message: String) : OcrUiState
    data class Success(
        val text: String,
        val characterCount: Int,
        val source: DocumentExtractionSource,
        val pageCount: Int,
        val totalPages: Int,
        val truncated: Boolean,
        val truncatedByPageLimit: Boolean,
        val truncatedByCharacterLimit: Boolean
    ) : OcrUiState
    data class Empty(
        val message: String,
        val truncated: Boolean = false,
        val truncatedByPageLimit: Boolean = false,
        val truncatedByCharacterLimit: Boolean = false,
        val pageCount: Int = 0
    ) : OcrUiState
    data class Error(val message: String) : OcrUiState
}

data class FileReviewUiState(
    val metadata: IncomingMetadata? = null,
    val editableNameWithoutExtension: String = "",
    val extension: String = "",
    val pdfThumbnail: Bitmap? = null,
    val errorMessage: String? = null,
    val ocrUiState: OcrUiState = OcrUiState.Idle
)

class FileReviewViewModel(
    private val pdfPreviewGenerator: PdfPreviewGenerator,
    private val documentAnalyzer: DocumentAnalyzer
) : ViewModel() {

    private var ocrJob: Job? = null
    private val _uiState = MutableStateFlow(FileReviewUiState())
    val uiState: StateFlow<FileReviewUiState> = _uiState.asStateFlow()

    fun setIncomingMetadata(metadata: IncomingMetadata) {
        cancelOcr()
        val baseName = FileNameUtils.stripExtension(metadata.displayName)
        _uiState.update {
            it.copy(
                metadata = metadata,
                editableNameWithoutExtension = baseName,
                extension = metadata.extension,
                pdfThumbnail = null,
                errorMessage = null,
                ocrUiState = OcrUiState.Idle
            )
        }

        if (metadata.mimeType == MimeTypeUtils.MIME_PDF || metadata.extension.equals("pdf", true)) {
            viewModelScope.launch {
                val thumb = pdfPreviewGenerator.generateFirstPageThumbnail(metadata.uri)
                _uiState.update { it.copy(pdfThumbnail = thumb) }
            }
        }

        val isImage = metadata.mimeType.startsWith("image/", ignoreCase = true)
        val isPdf = metadata.mimeType == MimeTypeUtils.MIME_PDF || metadata.extension.equals("pdf", true)
        if (isImage || isPdf) {
            startOcr(metadata)
        } else {
            _uiState.update {
                it.copy(
                    ocrUiState = OcrUiState.Error("Text extraction is not available for this file type.")
                )
            }
        }
    }

    private fun startOcr(metadata: IncomingMetadata) {
        ocrJob = viewModelScope.launch {
            _uiState.update {
                it.copy(ocrUiState = OcrUiState.Processing("Extracting text from the file…"))
            }
            try {
                val result = documentAnalyzer.analyze(metadata.uri, metadata.mimeType)
                val nextState = mapResultToUiState(result)
                _uiState.update { it.copy(ocrUiState = nextState) }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(ocrUiState = OcrUiState.Error("Text extraction failed unexpectedly."))
                }
            }
        }
    }

    fun cancelOcr() {
        ocrJob?.cancel()
        _uiState.update { it.copy(ocrUiState = OcrUiState.Idle) }
    }

    private fun mapResultToUiState(result: DocumentAnalysisResult): OcrUiState {
        return when (result.status) {
            DocumentAnalysisStatus.SUCCESS -> {
                val text = result.extractedText.orEmpty()
                if (text.isEmpty()) {
                    OcrUiState.Empty("No readable text was found.")
                } else {
                    OcrUiState.Success(
                        text = text,
                        characterCount = result.characterCount,
                        source = result.source,
                        pageCount = result.pageCount,
                        totalPages = result.totalPages,
                        truncated = result.truncated,
                        truncatedByPageLimit = result.truncatedByPageLimit,
                        truncatedByCharacterLimit = result.truncatedByCharacterLimit
                    )
                }
            }
            DocumentAnalysisStatus.EMPTY -> OcrUiState.Empty(
                message = result.errorMessage ?: "No readable text was found.",
                truncated = result.truncated,
                truncatedByPageLimit = result.truncatedByPageLimit,
                truncatedByCharacterLimit = result.truncatedByCharacterLimit,
                pageCount = result.pageCount
            )
            DocumentAnalysisStatus.UNSUPPORTED_TYPE -> OcrUiState.Error(result.errorMessage ?: "This file type is not supported for OCR.")
            DocumentAnalysisStatus.PERMISSION_DENIED -> OcrUiState.Error(result.errorMessage ?: "Permission to read the file was denied.")
            DocumentAnalysisStatus.UNREADABLE_FILE -> OcrUiState.Error(result.errorMessage ?: "The file could not be read or is corrupted.")
            DocumentAnalysisStatus.PASSWORD_PROTECTED_PDF -> OcrUiState.Error("This PDF is password-protected and cannot be processed.")
            DocumentAnalysisStatus.FILE_TOO_LARGE -> OcrUiState.Error("This file is too large to process in this version.")
            DocumentAnalysisStatus.MODEL_UNAVAILABLE -> OcrUiState.Error("The OCR model is not available yet. Please try again.")
            DocumentAnalysisStatus.CANCELLED -> OcrUiState.Error("Text extraction was cancelled.")
            DocumentAnalysisStatus.UNEXPECTED_ERROR -> OcrUiState.Error(result.errorMessage ?: "Text extraction failed unexpectedly.")
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

    override fun onCleared() {
        super.onCleared()
        cancelOcr()
    }
}
