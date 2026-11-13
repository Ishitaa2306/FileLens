package com.example.intelligentfileorganizer.util

object MimeTypeUtils {
    const val MIME_PDF = "application/pdf"
    const val MIME_DOC = "application/msword"
    const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val MIME_JPEG = "image/jpeg"
    const val MIME_PNG = "image/png"

    val SUPPORTED_MIME_TYPES = setOf(
        MIME_PDF,
        MIME_DOC,
        MIME_DOCX,
        MIME_JPEG,
        MIME_PNG
    )

    fun isSupported(mimeType: String?, filename: String? = null): Boolean {
        if (mimeType != null && SUPPORTED_MIME_TYPES.contains(mimeType.lowercase())) {
            return true
        }
        val ext = filename?.substringAfterLast('.', "")?.lowercase() ?: ""
        return when (ext) {
            "pdf", "doc", "docx", "jpg", "jpeg", "png" -> true
            else -> false
        }
    }

    fun resolveMimeType(declaredMime: String?, filename: String?): String {
        if (!declaredMime.isNullOrBlank() && declaredMime != "application/octet-stream") {
            return declaredMime
        }
        val ext = filename?.substringAfterLast('.', "")?.lowercase() ?: ""
        return when (ext) {
            "pdf" -> MIME_PDF
            "doc" -> MIME_DOC
            "docx" -> MIME_DOCX
            "jpg", "jpeg" -> MIME_JPEG
            "png" -> MIME_PNG
            else -> declaredMime ?: "application/octet-stream"
        }
    }

    fun getReadableType(mimeType: String?, extension: String?): String {
        return when {
            mimeType == MIME_PDF || extension.equals("pdf", ignoreCase = true) -> "PDF Document"
            mimeType == MIME_DOC || extension.equals("doc", ignoreCase = true) -> "Word Document (DOC)"
            mimeType == MIME_DOCX || extension.equals("docx", ignoreCase = true) -> "Word Document (DOCX)"
            mimeType == MIME_JPEG || extension.equals("jpg", ignoreCase = true) || extension.equals("jpeg", ignoreCase = true) -> "JPEG Image"
            mimeType == MIME_PNG || extension.equals("png", ignoreCase = true) -> "PNG Image"
            else -> "Unsupported Document"
        }
    }

    fun isImage(mimeType: String?): Boolean {
        return mimeType?.startsWith("image/") == true
    }
}

