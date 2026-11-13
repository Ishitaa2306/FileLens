package com.example.intelligentfileorganizer.domain.model

data class FolderItem(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val documentId: String,
    val documentUri: String,
    val createdAt: Long = System.currentTimeMillis(),
    val subfoldersCount: Int = 0,
    val filesCount: Int = 0
)

