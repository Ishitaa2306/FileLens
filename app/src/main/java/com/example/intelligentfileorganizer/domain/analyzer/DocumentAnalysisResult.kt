package com.example.intelligentfileorganizer.domain.analyzer

data class DocumentAnalysisResult(
    val extractedText: String? = null,
    val suggestedTopic: String? = null,
    val confidence: Float? = null
)

