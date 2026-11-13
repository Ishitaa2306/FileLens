package com.example.intelligentfileorganizer.domain.analyzer

import android.net.Uri

/**
 * Extension interface for Phase 2: Document Intelligence.
 * In Phase 1, this interface serves as an architectural extension point.
 */
interface DocumentAnalyzer {
    suspend fun analyze(
        documentUri: Uri,
        mimeType: String
    ): DocumentAnalysisResult
}

