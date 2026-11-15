package com.example.intelligentfileorganizer.domain.analyzer

enum class DocumentAnalysisStatus {
    SUCCESS,
    EMPTY,
    UNSUPPORTED_TYPE,
    UNREADABLE_FILE,
    PASSWORD_PROTECTED_PDF,
    FILE_TOO_LARGE,
    PERMISSION_DENIED,
    MODEL_UNAVAILABLE,
    CANCELLED,
    UNEXPECTED_ERROR
}

/**
 * PDF types are based on processed nonblank pages: text-only pages mean PDF_TEXT_LAYER,
 * OCR-required pages mean PDF_OCR, both mean PDF_MIXED, and all-blank/no pages mean PDF_EMPTY.
 */
enum class DocumentExtractionSource {
    IMAGE_OCR,
    PDF_TEXT_LAYER,
    PDF_OCR,
    PDF_MIXED,
    PDF_EMPTY,
    UNSPECIFIED
}

enum class DocumentPageExtractionSource {
    PDF_TEXT_LAYER,
    PDF_OCR,
    BLANK
}

data class DocumentPageProvenance(
    val pageNumber: Int,
    val source: DocumentPageExtractionSource
)

data class DocumentAnalysisResult(
    val extractedText: String? = null,
    val suggestedTopic: String? = null,
    val confidence: Float? = null,
    val status: DocumentAnalysisStatus = DocumentAnalysisStatus.SUCCESS,
    val source: DocumentExtractionSource = DocumentExtractionSource.UNSPECIFIED,
    val pageCount: Int = 1,
    val totalPages: Int = 1,
    val pageProvenance: List<DocumentPageProvenance> = emptyList(),
    val truncated: Boolean = false,
    val truncatedByPageLimit: Boolean = false,
    val truncatedByCharacterLimit: Boolean = false,
    val characterCount: Int = extractedText?.length ?: 0,
    val processingDurationMs: Long = 0L,
    val errorMessage: String? = null
)

