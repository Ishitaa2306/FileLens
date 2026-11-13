package com.example.intelligentfileorganizer.domain.model

data class FileItem(
    val id: Long = 0,
    val originalName: String,
    val storedName: String,
    val mimeType: String,
    val extension: String,
    val sizeBytes: Long,
    val documentUri: String,
    val documentId: String,
    val folderId: Long,
    val folderPath: String = "",
    val importedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

